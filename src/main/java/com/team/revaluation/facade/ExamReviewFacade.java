package com.team.revaluation.facade;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.model.Payment;
import com.team.revaluation.service.ReviewService;
import com.team.revaluation.service.PaymentService;
import com.team.revaluation.service.NotificationService;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ExamReviewFacade {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private UserRepository userRepository;

    public Map<String, Object> applyAndPay(Long studentId, Long scriptId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Step 1: Create review request
            Student student = (Student) userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));
            
            AnswerScript script = answerScriptRepository.findById(scriptId)
                    .orElseThrow(() -> new RuntimeException("Script not found"));
            
            // Check if script is eligible for review
            if (!script.getStatus().equals("EVALUATED") && !script.getStatus().equals("RESULTS_PUBLISHED")) {
                throw new RuntimeException("Script is not eligible for review. Current status: " + script.getStatus());
            }
            
            ReviewRequest request = new ReviewRequest();
            request.setStudent(student);
            request.setAnswerScript(script);
            
            ReviewRequest createdRequest = reviewService.applyForReview(request);
            result.put("reviewId", createdRequest.getReviewId());
            result.put("reviewFee", createdRequest.getReviewFee());
            result.put("reviewStatus", createdRequest.getReviewStatus());
            result.put("message", "Review request created successfully");
            
            // Step 2: Process payment automatically
            Payment payment = new Payment();
            payment.setAmount(createdRequest.getReviewFee());
            payment.setPaymentType("FULL");
            payment.setPaymentStatus("PENDING");
            payment.setStudent(student);
            
            Payment processedPayment = paymentService.processPayment(payment);
            result.put("paymentId", processedPayment.getPaymentId());
            result.put("paymentStatus", processedPayment.getPaymentStatus());
            
            // Step 3: Update request status if payment successful
            if ("SUCCESS".equals(processedPayment.getPaymentStatus())) {
                ReviewRequest updatedRequest = reviewService.processPaymentForReview(createdRequest.getReviewId());
                result.put("reviewStatus", updatedRequest.getReviewStatus());
                result.put("finalStatus", "PAYMENT_SUCCESS");
                result.put("message", "Review application and payment completed successfully");
            } else {
                result.put("message", "Payment failed. Please try again.");
                result.put("finalStatus", "PAYMENT_FAILED");
            }
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("message", "Failed to process review application: " + e.getMessage());
            result.put("finalStatus", "FAILED");
        }
        
        return result;
    }
}