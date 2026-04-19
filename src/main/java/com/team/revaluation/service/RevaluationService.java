package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.model.Payment;
import com.team.revaluation.model.Revaluator;
import com.team.revaluation.model.User;
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
public class RevaluationService implements IRevaluationService {

    @Autowired private RevaluationRequestRepository revaluationRepo;
    @Autowired private AnswerScriptRepository answerScriptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private PaymentService paymentService;

    @Autowired
    @Qualifier("fullRevaluationFeeStrategy")
    private FeeCalculationStrategy revaluationFeeStrategy;

    // ==================== APPLY ====================

    @Transactional
    @Override
    public RevaluationRequest applyForRevaluation(Long scriptId, Long studentId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        Student student = (Student) userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        // Script must be in AWAIT_STUDENT_DECISION or RESULTS_PUBLISHED/EVALUATED for revaluation
        String scriptStatus = script.getStatus();
        boolean eligible = "AWAIT_STUDENT_DECISION".equals(scriptStatus)
                        || "RESULTS_PUBLISHED".equals(scriptStatus)
                        || "EVALUATED".equals(scriptStatus);
        if (!eligible) {
            throw new RuntimeException(
                "Script is not eligible for revaluation. Current status: " + scriptStatus);
        }

        // Guard against duplicate active requests
        List<RevaluationRequest> existingRequests = revaluationRepo.findByStudentUserId(studentId);
        for (RevaluationRequest req : existingRequests) {
            if (req.getAnswerScript().getScriptId().equals(scriptId) &&
                !req.getRevaluationStatus().equals("CANCELLED") &&
                !req.getRevaluationStatus().equals("REJECTED") &&
                !req.getRevaluationStatus().equals("REVALUATION_COMPLETED")) {
                throw new RuntimeException("A revaluation request already exists for this script. Status: "
                    + req.getRevaluationStatus());
            }
        }

        RevaluationRequest request = new RevaluationRequest();
        request.setStudent(student);
        request.setAnswerScript(script);
        request.setRevaluationFee(revaluationFeeStrategy.calculateFee());

        // RevaluationRequest starts at PAYMENT_PENDING
        RevaluationRequestStateMachine.transition(request, "PAYMENT_PENDING");

        // FIX: AnswerScript must also mirror: AWAIT_STUDENT_DECISION → REVALUATION_REQUESTED (§6 row 10)
        // Then: REVALUATION_REQUESTED → REVALUATION_PAYMENT_PENDING (§6 row 11)
        try {
            if ("AWAIT_STUDENT_DECISION".equals(scriptStatus)) {
                AnswerScriptStateMachine.transition(script, "REVALUATION_REQUESTED");
                AnswerScriptStateMachine.transition(script, "REVALUATION_PAYMENT_PENDING");
                answerScriptRepository.save(script);
            }
        } catch (InvalidStateTransitionException e) {
            System.err.println("[RevaluationService] applyForRevaluation: script state warning: " + e.getMessage());
        }

        RevaluationRequest savedRequest = revaluationRepo.save(request);

        notificationService.notifyStudent(student,
            "Revaluation request #" + savedRequest.getRevaluationId()
                + " created. Fee: \u20b9" + revaluationFeeStrategy.calculateFee()
                + ". Please complete payment.");

        return savedRequest;
    }

    // ==================== PAYMENT ====================

    /**
     * On success transitions:
     *   RevaluationRequest: PAYMENT_PENDING → REVALUATION_IN_PROGRESS
     *   AnswerScript:       AWAIT_STUDENT_DECISION → REVALUATION_REQUESTED → REVALUATION_PAYMENT_PENDING → REVALUATION_IN_PROGRESS
     *                       OR REVALUATION_PAYMENT_PENDING → REVALUATION_IN_PROGRESS
     */
    @Transactional
    @Override
    public RevaluationRequest processRevaluationPayment(Long revaluationId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        if (!"PAYMENT_PENDING".equals(request.getRevaluationStatus())) {
            throw new RuntimeException("Cannot process payment. Request is in status: "
                + request.getRevaluationStatus());
        }

        Payment payment = new Payment();
        payment.setAmount(request.getRevaluationFee());
        payment.setPaymentType("FULL");
        payment.setPaymentStatus("PENDING");
        payment.setStudent(request.getStudent());

        Payment processedPayment = paymentService.processPayment(payment);

        if ("SUCCESS".equals(processedPayment.getPaymentStatus())) {
            RevaluationRequestStateMachine.transition(request, "REVALUATION_IN_PROGRESS");

            // FIX: Handle both starting states
            AnswerScript script = request.getAnswerScript();
            if (script != null) {
                try {
                    String currentStatus = script.getStatus();
                    
                    if ("AWAIT_STUDENT_DECISION".equals(currentStatus)) {
                        // Full path: AWAIT_STUDENT_DECISION → REVALUATION_REQUESTED → REVALUATION_PAYMENT_PENDING → REVALUATION_IN_PROGRESS
                        AnswerScriptStateMachine.transition(script, "REVALUATION_REQUESTED");
                        AnswerScriptStateMachine.transition(script, "REVALUATION_PAYMENT_PENDING");
                        AnswerScriptStateMachine.transition(script, "REVALUATION_IN_PROGRESS");
                    } else if ("REVALUATION_PAYMENT_PENDING".equals(currentStatus)) {
                        // Already at payment pending, just move to in progress
                        AnswerScriptStateMachine.transition(script, "REVALUATION_IN_PROGRESS");
                    } else if ("RESULTS_PUBLISHED".equals(currentStatus) || "EVALUATED".equals(currentStatus)) {
                        // Handle scripts that bypassed review workflow
                        AnswerScriptStateMachine.transition(script, "REVALUATION_REQUESTED");
                        AnswerScriptStateMachine.transition(script, "REVALUATION_PAYMENT_PENDING");
                        AnswerScriptStateMachine.transition(script, "REVALUATION_IN_PROGRESS");
                    } else {
                        throw new RuntimeException("Script in invalid state for revaluation payment: " + currentStatus);
                    }
                    answerScriptRepository.save(script);
                } catch (InvalidStateTransitionException e) {
                    throw new RuntimeException("Invalid script state transition: " + e.getMessage());
                }
            }

            RevaluationRequest savedRequest = revaluationRepo.save(request);
            notificationService.notifyStudent(request.getStudent(),
                "\u2705 Payment successful! Revaluation request #" + revaluationId
                    + " is now IN PROGRESS. A revaluator will be assigned shortly.");
            notificationService.notifyRevaluationStatusChange(savedRequest);
            return savedRequest;

        } else {
            RevaluationRequestStateMachine.transition(request, "PAYMENT_FAILED");
            notificationService.notifyStudent(request.getStudent(),
                "\u274c Payment failed for revaluation request #" + revaluationId + ". Please try again.");
            return revaluationRepo.save(request);
        }
    }

    // ==================== READ ====================

    @Override
    public RevaluationRequest getRevaluationById(Long revaluationId) {
        return revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found with id: " + revaluationId));
    }

    @Override
    public List<RevaluationRequest> getRevaluationsByStudent(Long studentId) {
        return revaluationRepo.findByStudentUserId(studentId);
    }

    @Override
    public List<RevaluationRequest> getAllRevaluations() {
        return revaluationRepo.findAll();
    }

    @Override
    public List<RevaluationRequest> getPendingRevaluations() {
        return revaluationRepo.findByRevaluationStatus("PAYMENT_PENDING");
    }

    public List<RevaluationRequest> getRevaluationsByStatusForRevaluator(String status, Long revaluatorId) {
        return revaluationRepo.findByRevaluatorUserIdAndRevaluationStatus(revaluatorId, status);
    }

    @Override
    public List<RevaluationRequest> getPendingForRevaluator() {
        return revaluationRepo.findPendingForRevaluator();
    }

    @Override
    public long countByStatus(String status) {
        return revaluationRepo.countByStatus(status);
    }

    // ==================== STATUS UPDATES ====================

    @Transactional
    @Override
    public RevaluationRequest updateRevaluationStatus(Long revaluationId, String newStatus) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        RevaluationRequestStateMachine.transition(request, newStatus);
        RevaluationRequest updatedRequest = revaluationRepo.save(request);
        notificationService.notifyRevaluationStatusChange(updatedRequest);
        return updatedRequest;
    }

    @Transactional
    @Override
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
    @Override
    public RevaluationRequest cancelRevaluationRequest(Long revaluationId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        RevaluationRequestStateMachine.transition(request, "CANCELLED");
        RevaluationRequest savedRequest = revaluationRepo.save(request);
        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " has been cancelled.");
        return savedRequest;
    }

    // ==================== REVALUATOR SUBMIT ====================

    /**
     * Satisfies: "PUT /revaluator/requests/{id}/submit → REVALUATION_COMPLETED + notifies student"
     *
     * AnswerScript: REVALUATION_IN_PROGRESS → REVALUATION_COMPLETED (§6 row 13)
     */
    @Transactional
    @Override
    public RevaluationRequest submitRevaluationMarks(Long revaluationId, Float marks, String comments) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found: " + revaluationId));

        if (!"REVALUATION_IN_PROGRESS".equals(request.getRevaluationStatus())) {
            throw new RuntimeException("Cannot submit marks. Status: "
                + request.getRevaluationStatus() + ". Expected: REVALUATION_IN_PROGRESS");
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
            // §6 row 13: REVALUATION_IN_PROGRESS → REVALUATION_COMPLETED
            AnswerScriptStateMachine.transition(script, "REVALUATION_COMPLETED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Invalid state transition: " + e.getMessage());
        }
        answerScriptRepository.save(script);

        RevaluationRequestStateMachine.transition(request, "REVALUATION_COMPLETED");
        RevaluationRequest savedRequest = revaluationRepo.save(request);

        String notificationMessage = String.format(
            "\u2705 Revaluation completed for Script #%d. Marks updated from %.2f to %.2f.%s",
            script.getScriptId(),
            oldMarks != null ? oldMarks : 0,
            marks,
            comments != null ? " Comments: " + comments : ""
        );
        notificationService.notifyStudent(request.getStudent(), notificationMessage);
        notificationService.notifyRevaluationStatusChange(savedRequest);

        return savedRequest;
    }

    // ==================== ADMIN ASSIGN ====================

    @Transactional
    @Override
    public Map<String, Object> assignRevaluatorToRequest(Long revaluationId, Long revaluatorId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
 
        // FIX: Accept REVALUATION_IN_PROGRESS (the state payment transitions to).
        // "VERIFIED" is not a valid revaluation state — removed that incorrect check.
        String currentStatus = request.getRevaluationStatus();
        if (!"REVALUATION_IN_PROGRESS".equals(currentStatus)) {
            throw new RuntimeException(
                "Cannot assign revaluator. Request must be in REVALUATION_IN_PROGRESS. Current: "
                    + currentStatus);
        }
 
        User user = userRepository.findById(revaluatorId)
                .orElseThrow(() -> new RuntimeException("Revaluator not found"));
 
        if (!"REVALUATOR".equals(user.getRole())) {
            throw new RuntimeException("User is not a revaluator");
        }
 
        Revaluator revaluator = (Revaluator) user;
        request.setRevaluator(revaluator);
        RevaluationRequest updatedRequest = revaluationRepo.save(request);
 
        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " assigned to revaluator: " + revaluator.getName());
 
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Revaluator assigned successfully");
        response.put("revaluationId", revaluationId);
        response.put("revaluatorId", revaluatorId);
        response.put("revaluatorName", revaluator.getName());
        response.put("status", updatedRequest.getRevaluationStatus());
        response.put("scriptStatus", request.getAnswerScript() != null
            ? request.getAnswerScript().getStatus() : "N/A");
        return response;
    }
}