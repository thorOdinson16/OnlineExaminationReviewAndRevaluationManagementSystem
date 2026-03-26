package com.team.revaluation.service;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.RevaluationRequestRepository;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional
    public RevaluationRequest applyForRevaluation(Long scriptId, Long studentId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        Student student = (Student) userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        // Check if script is eligible for revaluation
        if (!script.getStatus().equals("EVALUATED") && !script.getStatus().equals("RESULTS_PUBLISHED")) {
            throw new RuntimeException("Script is not eligible for revaluation. Current status: " + script.getStatus());
        }

        // Check if revaluation request already exists and is not cancelled/rejected
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
        request.setRevaluationFee(1500.0f);
        request.setRevaluationStatus("PAYMENT_PENDING");

        RevaluationRequest savedRequest = revaluationRepo.save(request);

        notificationService.notifyStudent(student,
            "Revaluation request #" + savedRequest.getRevaluationId() + " created. Fee: ₹1500. Please complete payment.");

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

        // Create payment record
        Payment payment = new Payment();
        payment.setAmount(request.getRevaluationFee());
        payment.setPaymentType("FULL");
        payment.setPaymentStatus("PENDING");
        payment.setStudent(request.getStudent());

        // Process payment
        Payment processedPayment = paymentService.processPayment(payment);

        if ("SUCCESS".equals(processedPayment.getPaymentStatus())) {
            request.setRevaluationStatus("PAYMENT_SUCCESS");

            // Update script status
            AnswerScript script = request.getAnswerScript();
            if (script != null) {
                script.setStatus("REVALUATION_REQUESTED");
                answerScriptRepository.save(script);
            }

            RevaluationRequest savedRequest = revaluationRepo.save(request);
            notificationService.notifyStudent(request.getStudent(),
                "✅ Payment successful! Revaluation request #" + revaluationId + " has been submitted for processing.");
            notificationService.notifyRevaluationStatusChange(savedRequest);

            return savedRequest;
        } else {
            request.setRevaluationStatus("PAYMENT_FAILED");
            notificationService.notifyStudent(request.getStudent(),
                "❌ Payment failed for revaluation request #" + revaluationId + ". Please try again.");
            return revaluationRepo.save(request);
        }
    }

    @Transactional
    public RevaluationRequest updateRevaluationStatus(Long revaluationId, String newStatus) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        String currentStatus = request.getRevaluationStatus();

        // Define valid state transitions
        boolean isValidTransition = false;

        switch (currentStatus) {
            case "PAYMENT_PENDING":
                isValidTransition = newStatus.equals("PAYMENT_SUCCESS") || 
                                    newStatus.equals("PAYMENT_FAILED") || 
                                    newStatus.equals("CANCELLED");
                break;
            case "PAYMENT_SUCCESS":
                isValidTransition = newStatus.equals("VERIFIED") || 
                                    newStatus.equals("REJECTED");
                break;
            case "VERIFIED":
                isValidTransition = newStatus.equals("REVALUATION_IN_PROGRESS");
                break;
            case "REVALUATION_IN_PROGRESS":
                isValidTransition = newStatus.equals("REVALUATION_COMPLETED");
                break;
            case "REVALUATION_COMPLETED":
                isValidTransition = false; // Final state
                break;
            case "CANCELLED":
                isValidTransition = false; // Final state
                break;
            case "REJECTED":
                isValidTransition = false; // Final state
                break;
            case "PAYMENT_FAILED":
                isValidTransition = newStatus.equals("PAYMENT_PENDING") || newStatus.equals("CANCELLED");
                break;
            default:
                isValidTransition = false;
        }

        if (!isValidTransition) {
            throw new RuntimeException("Invalid state transition from " + currentStatus + " to " + newStatus);
        }

        request.setRevaluationStatus(newStatus);
        RevaluationRequest updatedRequest = revaluationRepo.save(request);

        notificationService.notifyRevaluationStatusChange(updatedRequest);

        return updatedRequest;
    }

    // Get revaluation requests by status (for revaluator)
    public List<RevaluationRequest> getRevaluationsByStatusForRevaluator(String status, Long revaluatorId) {
        return revaluationRepo.findByRevaluatorUserIdAndRevaluationStatus(revaluatorId, status);
    }

    // Get pending revaluation requests for revaluator
    public List<RevaluationRequest> getPendingForRevaluator() {
        return revaluationRepo.findPendingForRevaluator();
    }

    // Count revaluations by status
    public long countByStatus(String status) {
        return revaluationRepo.countByStatus(status);
    }

    // Verify revaluation request (admin)
    @Transactional
    public RevaluationRequest verifyRevaluation(Long revaluationId) {
        return updateRevaluationStatus(revaluationId, "VERIFIED");
    }

    // Reject revaluation request
    @Transactional
    public RevaluationRequest rejectRevaluation(Long revaluationId, String reason) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        request.setRevaluationStatus("REJECTED");
        request.setRejectionReason(reason);
        RevaluationRequest savedRequest = revaluationRepo.save(request);

        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " has been rejected. Reason: " + reason);

        return savedRequest;
    }

    // Cancel a revaluation request (only if payment not yet processed)
    @Transactional
    public RevaluationRequest cancelRevaluationRequest(Long revaluationId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));

        if (!"PAYMENT_PENDING".equals(request.getRevaluationStatus())) {
            throw new RuntimeException("Cannot cancel revaluation request in status: " + request.getRevaluationStatus());
        }

        request.setRevaluationStatus("CANCELLED");
        RevaluationRequest savedRequest = revaluationRepo.save(request);

        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " has been cancelled.");

        return savedRequest;
    }
}