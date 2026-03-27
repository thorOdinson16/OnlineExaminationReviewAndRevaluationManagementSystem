package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.RevaluationRequestRepository;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class RevaluationService {

    @Autowired
    private RevaluationRequestRepository revaluationRepo;

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    @Qualifier("fullRevaluationFeeStrategy")
    private FeeCalculationStrategy revaluationFeeStrategy;

    @Transactional
    public RevaluationRequest applyForRevaluation(Long scriptId, Long studentId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        Student student = (Student) userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        if (!script.getStatus().equals("EVALUATED") && !script.getStatus().equals("RESULTS_PUBLISHED")) {
            throw new RuntimeException("Script is not eligible for revaluation. Current status: " + script.getStatus());
        }

        List<RevaluationRequest> existingRequests = revaluationRepo.findByStudentUserId(studentId);
        for (RevaluationRequest req : existingRequests) {
            if (req.getAnswerScript().getScriptId().equals(scriptId) &&
                !req.getRevaluationStatus().equals("CANCELLED") &&
                !req.getRevaluationStatus().equals("REJECTED") &&
                !req.getRevaluationStatus().equals("REVALUATION_COMPLETED")) {
                throw new RuntimeException("A revaluation request already exists for this script with status: " + req.getRevaluationStatus());
            }
        }

        RevaluationRequest request = new RevaluationRequest();
        request.setStudent(student);
        request.setAnswerScript(script);
        request.setRevaluationFee(revaluationFeeStrategy.calculateFee());
        
        // ✅ Use state machine
        RevaluationRequestStateMachine.transition(request, "PAYMENT_PENDING");

        RevaluationRequest savedRequest = revaluationRepo.save(request);

        notificationService.notifyStudent(student,
            "Revaluation request #" + savedRequest.getRevaluationId() + " created. Fee: ₹" + revaluationFeeStrategy.calculateFee() + ". Please complete payment.");

        return savedRequest;
    }

    public RevaluationRequest getRevaluationById(Long revaluationId) {
        return revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found with id: " + revaluationId));
    }

    public List<RevaluationRequest> getRevaluationsByStudent(Long studentId) {
        return revaluationRepo.findByStudentUserId(studentId);
    }

    public List<RevaluationRequest> getAllRevaluations() {
        return revaluationRepo.findAll();
    }

    public List<RevaluationRequest> getPendingRevaluations() {
        return revaluationRepo.findByRevaluationStatus("PAYMENT_PENDING");
    }

    @Transactional
    public RevaluationRequest processRevaluationPayment(Long revaluationId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        if (!"PAYMENT_PENDING".equals(request.getRevaluationStatus())) {
            throw new RuntimeException("Cannot process payment. Request is in status: " + request.getRevaluationStatus());
        }

        Payment payment = new Payment();
        payment.setAmount(request.getRevaluationFee());
        payment.setPaymentType("FULL");
        payment.setPaymentStatus("PENDING");
        payment.setStudent(request.getStudent());

        Payment processedPayment = paymentService.processPayment(payment);

        if ("SUCCESS".equals(processedPayment.getPaymentStatus())) {
            // ✅ Use state machine
            RevaluationRequestStateMachine.transition(request, "PAYMENT_SUCCESS");

            AnswerScript script = request.getAnswerScript();
            if (script != null) {
                try {
                    AnswerScriptStateMachine.transition(script, "REVALUATION_REQUESTED");
                } catch (InvalidStateTransitionException e) {
                    throw new RuntimeException("Invalid state transition: " + e.getMessage());
                }
                answerScriptRepository.save(script);
            }

            RevaluationRequest savedRequest = revaluationRepo.save(request);
            notificationService.notifyStudent(request.getStudent(),
                "✅ Payment successful! Revaluation request #" + revaluationId + " has been submitted for processing.");
            notificationService.notifyRevaluationStatusChange(savedRequest);

            return savedRequest;
        } else {
            // ✅ Use state machine
            RevaluationRequestStateMachine.transition(request, "PAYMENT_FAILED");
            notificationService.notifyStudent(request.getStudent(),
                "❌ Payment failed for revaluation request #" + revaluationId + ". Please try again.");
            return revaluationRepo.save(request);
        }
    }

    @Transactional
    public RevaluationRequest updateRevaluationStatus(Long revaluationId, String newStatus) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        // ✅ Use state machine instead of manual validation
        RevaluationRequestStateMachine.transition(request, newStatus);
        
        RevaluationRequest updatedRequest = revaluationRepo.save(request);
        notificationService.notifyRevaluationStatusChange(updatedRequest);

        return updatedRequest;
    }

    public List<RevaluationRequest> getRevaluationsByStatusForRevaluator(String status, Long revaluatorId) {
        return revaluationRepo.findByRevaluatorUserIdAndRevaluationStatus(revaluatorId, status);
    }

    public List<RevaluationRequest> getPendingForRevaluator() {
        return revaluationRepo.findPendingForRevaluator();
    }

    public long countByStatus(String status) {
        return revaluationRepo.countByStatus(status);
    }

    @Transactional
    public RevaluationRequest verifyRevaluation(Long revaluationId) {
        return updateRevaluationStatus(revaluationId, "VERIFIED");
    }

    @Transactional
    public RevaluationRequest rejectRevaluation(Long revaluationId, String reason) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        RevaluationRequestStateMachine.transition(request, "REJECTED");
        request.setRejectionReason(reason);
        RevaluationRequest savedRequest = revaluationRepo.save(request);

        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " has been rejected. Reason: " + reason);

        return savedRequest;
    }

    @Transactional
    public RevaluationRequest cancelRevaluationRequest(Long revaluationId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        RevaluationRequestStateMachine.transition(request, "CANCELLED");
        RevaluationRequest savedRequest = revaluationRepo.save(request);

        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " has been cancelled.");

        return savedRequest;
    }

    @Transactional
    public RevaluationRequest submitRevaluationMarks(Long revaluationId, Float marks, String comments) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found with id: " + revaluationId));

        // ✅ Validate status via state machine check
        if (!"REVALUATION_IN_PROGRESS".equals(request.getRevaluationStatus())) {
            throw new RuntimeException("Cannot submit marks. Request is in status: " + 
                request.getRevaluationStatus() + ". Expected: REVALUATION_IN_PROGRESS");
        }

        if (marks == null || marks < 0) {
            throw new RuntimeException("Invalid marks: " + marks + ". Marks must be non-negative.");
        }

        AnswerScript script = request.getAnswerScript();
        if (script == null) {
            throw new RuntimeException("Answer script not found for revaluation request #" + revaluationId);
        }

        Float oldMarks = script.getTotalMarks();
        script.setTotalMarks(marks);

        try {
            AnswerScriptStateMachine.transition(script, "REVALUATION_COMPLETED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Invalid state transition: " + e.getMessage());
        }

        answerScriptRepository.save(script);

        // ✅ Use state machine
        RevaluationRequestStateMachine.transition(request, "REVALUATION_COMPLETED");
        RevaluationRequest savedRequest = revaluationRepo.save(request);

        String notificationMessage = String.format(
            "✅ Revaluation completed for Script #%d. Marks updated from %.2f to %.2f.%s",
            script.getScriptId(), 
            oldMarks != null ? oldMarks : 0, 
            marks,
            comments != null ? " Comments: " + comments : ""
        );
        
        notificationService.notifyStudent(request.getStudent(), notificationMessage);
        notificationService.notifyRevaluationStatusChange(savedRequest);

        return savedRequest;
    }
    
    @Transactional
    public Map<String, Object> assignRevaluatorToRequest(Long revaluationId, Long revaluatorId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
        
        // ✅ Validate current status using state machine allowed transitions
        if (!RevaluationRequestStateMachine.isTransitionAllowed(request.getRevaluationStatus(), "REVALUATION_IN_PROGRESS")) {
            throw new RuntimeException(
                "Cannot assign revaluator. Request is in status: " + request.getRevaluationStatus()
            );
        }
        
        User user = userRepository.findById(revaluatorId)
                .orElseThrow(() -> new RuntimeException("Revaluator not found"));
        
        if (!"REVALUATOR".equals(user.getRole())) {
            throw new RuntimeException("User is not a revaluator");
        }
        
        Revaluator revaluator = (Revaluator) user;
        request.setRevaluator(revaluator);
        
        // ✅ Use state machine
        RevaluationRequestStateMachine.transition(request, "REVALUATION_IN_PROGRESS");
        
        AnswerScript script = request.getAnswerScript();
        if (script != null) {
            try {
                if ("REVALUATION_REQUESTED".equals(script.getStatus()) || 
                    "RESULTS_PUBLISHED".equals(script.getStatus())) {
                    AnswerScriptStateMachine.transition(script, "REVALUATION_IN_PROGRESS");
                    answerScriptRepository.save(script);
                }
            } catch (InvalidStateTransitionException e) {
                System.err.println("Warning: Could not update script status: " + e.getMessage());
            }
        }
        
        RevaluationRequest updatedRequest = revaluationRepo.save(request);
        
        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " assigned to revaluator: " + revaluator.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Revaluator assigned successfully");
        response.put("revaluationId", revaluationId);
        response.put("revaluatorId", revaluatorId);
        response.put("revaluatorName", revaluator.getName());
        response.put("status", updatedRequest.getRevaluationStatus());
        response.put("scriptStatus", script != null ? script.getStatus() : "N/A");
        
        return response;
    }
}