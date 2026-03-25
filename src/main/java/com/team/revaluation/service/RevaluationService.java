// File: src/main/java/com/team/revaluation/service/RevaluationService.java
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
    private AnswerScriptRepository scriptRepo;
    
    @Autowired
    private UserRepository userRepo;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public RevaluationRequest applyForRevaluation(Long scriptId, Long studentId) {
        AnswerScript script = scriptRepo.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        Student student = (Student) userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        // Check if script is eligible for revaluation
        if (!script.getStatus().equals("EVALUATED") && !script.getStatus().equals("RESULTS_PUBLISHED")) {
            throw new RuntimeException("Script is not eligible for revaluation. Current status: " + script.getStatus());
        }
        
        // Check if revaluation already requested for this script
        // This is a simplified check - in production, you'd need a custom query
        List<RevaluationRequest> existingRequests = revaluationRepo.findByStudentUserId(studentId);
        for (RevaluationRequest existing : existingRequests) {
            if (existing.getAnswerScript().getScriptId().equals(scriptId) && 
                !"COMPLETED".equals(existing.getRevaluationStatus()) &&
                !"REJECTED".equals(existing.getRevaluationStatus())) {
                throw new RuntimeException("A revaluation request already exists for this script");
            }
        }

        RevaluationRequest request = new RevaluationRequest();
        request.setAnswerScript(script);
        request.setStudent(student);
        
        // Use Strategy Pattern to set revaluation fee
        FeeCalculationStrategy feeStrategy = new FullRevaluationFeeStrategy();
        request.setRevaluationFee(feeStrategy.calculateFee());
        request.setRevaluationStatus("PAYMENT_PENDING");
        
        RevaluationRequest savedRequest = revaluationRepo.save(request);
        
        // Notify student about application
        notificationService.notifyStudent(student, 
            "Revaluation application submitted for Script #" + scriptId + ". Fee: ₹" + request.getRevaluationFee());
        
        return savedRequest;
    }
    
    @Transactional
    public RevaluationRequest processRevaluationPayment(Long revaluationId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
        
        if (!"PAYMENT_PENDING".equals(request.getRevaluationStatus())) {
            throw new RuntimeException("Revaluation request is not in PAYMENT_PENDING state. Current status: " + request.getRevaluationStatus());
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
            request.setRevaluationStatus("REVALUATION_IN_PROGRESS");
            
            // Update script status
            AnswerScript script = request.getAnswerScript();
            if (script != null) {
                script.setStatus("REVALUATION_IN_PROGRESS");
                scriptRepo.save(script);
            }
            
            RevaluationRequest savedRequest = revaluationRepo.save(request);
            
            // Notify student about status change
            notificationService.notifyRevaluationStatusChange(savedRequest);
            
            return savedRequest;
        } else {
            request.setRevaluationStatus("PAYMENT_FAILED");
            RevaluationRequest savedRequest = revaluationRepo.save(request);
            
            notificationService.notifyStudent(request.getStudent(),
                "Payment failed for revaluation request #" + revaluationId + ". Please try again.");
            
            return savedRequest;
        }
    }
    
    public RevaluationRequest getRevaluationById(Long revaluationId) {
        return revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
    }
    
    public List<RevaluationRequest> getAllRevaluations() {
        return revaluationRepo.findAll();
    }
    
    public List<RevaluationRequest> getRevaluationsByStudent(Long studentId) {
        return revaluationRepo.findByStudentUserId(studentId);
    }
    
    public List<RevaluationRequest> getRevaluationsByStatus(String status) {
        return revaluationRepo.findByRevaluationStatus(status);
    }
    
    public List<RevaluationRequest> getPendingRevaluations() {
        return revaluationRepo.findByRevaluationStatus("PAYMENT_PENDING");
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
                isValidTransition = newStatus.equals("REVALUATION_IN_PROGRESS") || newStatus.equals("PAYMENT_FAILED") || newStatus.equals("CANCELLED");
                break;
            case "PAYMENT_FAILED":
                isValidTransition = newStatus.equals("PAYMENT_PENDING"); // Retry payment
                break;
            case "REVALUATION_IN_PROGRESS":
                isValidTransition = newStatus.equals("REVALUATION_COMPLETED");
                break;
            case "REVALUATION_COMPLETED":
                isValidTransition = false; // Final state
                break;
            case "VERIFIED":
                isValidTransition = newStatus.equals("REVALUATION_IN_PROGRESS");
                break;
            default:
                isValidTransition = false;
        }
        
        if (!isValidTransition) {
            throw new RuntimeException("Invalid state transition from " + currentStatus + " to " + newStatus);
        }
        
        request.setRevaluationStatus(newStatus);
        RevaluationRequest savedRequest = revaluationRepo.save(request);
        
        // Notify student about status change
        notificationService.notifyRevaluationStatusChange(savedRequest);
        
        // If completed, update script marks
        if ("REVALUATION_COMPLETED".equals(newStatus)) {
            // In a real system, the revaluator would submit new marks
            // This is handled by the RevaluatorController
        }
        
        return savedRequest;
    }
    
    @Transactional
    public RevaluationRequest assignRevaluator(Long revaluationId, Long revaluatorId) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
        
        com.team.revaluation.model.Revaluator revaluator = 
            (com.team.revaluation.model.Revaluator) userRepo.findById(revaluatorId)
                .orElseThrow(() -> new RuntimeException("Revaluator not found"));
        
        request.setRevaluator(revaluator);
        
        // Update status if still pending
        if ("PAYMENT_SUCCESS".equals(request.getRevaluationStatus()) || 
            "PAYMENT_PENDING".equals(request.getRevaluationStatus())) {
            request.setRevaluationStatus("ASSIGNED");
        }
        
        RevaluationRequest savedRequest = revaluationRepo.save(request);
        
        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " has been assigned to revaluator: " + revaluator.getName());
        
        return savedRequest;
    }
    
    @Transactional
    public RevaluationRequest submitRevaluationMarks(Long revaluationId, Float newMarks) {
        RevaluationRequest request = revaluationRepo.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
        
        if (!"REVALUATION_IN_PROGRESS".equals(request.getRevaluationStatus())) {
            throw new RuntimeException("Cannot submit marks for revaluation in status: " + request.getRevaluationStatus());
        }
        
        // Update script marks
        AnswerScript script = request.getAnswerScript();
        Float oldMarks = script.getTotalMarks();
        script.setTotalMarks(newMarks);
        script.setStatus("REVALUATION_COMPLETED");
        scriptRepo.save(script);
        
        // Update request status
        request.setRevaluationStatus("REVALUATION_COMPLETED");
        RevaluationRequest savedRequest = revaluationRepo.save(request);
        
        // Notify student
        notificationService.notifyStudent(request.getStudent(),
            "Revaluation completed for Script #" + script.getScriptId() + 
            ". Marks changed from " + oldMarks + " to " + newMarks);
        
        return savedRequest;
    }
}