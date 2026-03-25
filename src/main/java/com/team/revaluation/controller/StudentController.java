// File: src/main/java/com/team/revaluation/controller/StudentController.java
package com.team.revaluation.controller;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.service.RevaluationService;
import com.team.revaluation.service.ReviewService;
import com.team.revaluation.service.PaymentService;
import com.team.revaluation.model.Payment;
import com.team.revaluation.facade.ExamReviewFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private RevaluationService revaluationService;

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ExamReviewFacade examReviewFacade;

    // ==================== REVIEW FLOW ENDPOINTS ====================
    
    // Apply for paper review - Using Facade
    @PostMapping("/review/apply")
    public ResponseEntity<Map<String, Object>> applyForReview(@RequestBody Map<String, Object> request) {
        try {
            Long studentId = ((Number) request.get("studentId")).longValue();
            
            // Fix: Properly parameterize the inner Map
            @SuppressWarnings("unchecked")
            Map<String, Object> answerScriptMap = (Map<String, Object>) request.get("answerScript");
            Long scriptId = ((Number) answerScriptMap.get("scriptId")).longValue();
            
            Map<String, Object> result = examReviewFacade.applyAndPay(studentId, scriptId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("message", "Failed to apply for review");
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get specific review by ID
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<ReviewRequest> getReviewById(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReviewById(reviewId));
    }

    // View all review requests by student
    @GetMapping("/review/student/{studentId}")
    public ResponseEntity<List<ReviewRequest>> getMyReviews(@PathVariable Long studentId) {
        return ResponseEntity.ok(reviewService.getReviewsByStudent(studentId));
    }

    // Pay for review request
    @PostMapping("/review/{reviewId}/pay")
    public ResponseEntity<ReviewRequest> payForReview(@PathVariable Long reviewId) {
        ReviewRequest updatedRequest = reviewService.processPaymentForReview(reviewId);
        return ResponseEntity.ok(updatedRequest);
    }

    // Cancel review request
    @DeleteMapping("/review/{reviewId}/cancel")
    public ResponseEntity<Map<String, String>> cancelReview(@PathVariable Long reviewId) {
        reviewService.cancelReviewRequest(reviewId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Review request cancelled successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== REVALUATION FLOW ENDPOINTS ====================
    
    // Apply for full revaluation
    @PostMapping("/revaluation/apply")
    public ResponseEntity<RevaluationRequest> applyForRevaluation(
            @RequestParam Long scriptId, 
            @RequestParam Long studentId) {
        return ResponseEntity.ok(revaluationService.applyForRevaluation(scriptId, studentId));
    }

    // Get specific revaluation by ID
    @GetMapping("/revaluation/{revaluationId}")
    public ResponseEntity<RevaluationRequest> getRevaluationById(@PathVariable Long revaluationId) {
        return ResponseEntity.ok(revaluationService.getRevaluationById(revaluationId));
    }

    // View all revaluation requests by student
    @GetMapping("/revaluation/student/{studentId}")
    public ResponseEntity<List<RevaluationRequest>> getMyRevaluations(@PathVariable Long studentId) {
        return ResponseEntity.ok(revaluationService.getRevaluationsByStudent(studentId));
    }

    // Pay for revaluation request
    @PostMapping("/revaluation/{revaluationId}/pay")
    public ResponseEntity<RevaluationRequest> payForRevaluation(@PathVariable Long revaluationId) {
        RevaluationRequest updatedRequest = revaluationService.processRevaluationPayment(revaluationId);
        return ResponseEntity.ok(updatedRequest);
    }

    // Cancel revaluation request
    @DeleteMapping("/revaluation/{revaluationId}/cancel")
    public ResponseEntity<Map<String, String>> cancelRevaluation(@PathVariable Long revaluationId) {
        revaluationService.updateRevaluationStatus(revaluationId, "CANCELLED");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Revaluation request cancelled successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== RESULTS ENDPOINTS ====================
    
    // Get student exam results
    @GetMapping("/results/{studentId}")
    public ResponseEntity<List<AnswerScript>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(answerScriptRepository.findByStudentUserId(studentId));
    }

    // Get specific result by script ID
    @GetMapping("/results/script/{scriptId}")
    public ResponseEntity<AnswerScript> getScriptResult(@PathVariable Long scriptId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        return ResponseEntity.ok(script);
    }

    // ==================== PAYMENT ENDPOINTS ====================
    
    // Get payment status
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<Payment> getPaymentStatus(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(payment);
    }

    // Get all payments by student
    @GetMapping("/payment/student/{studentId}")
    public ResponseEntity<List<Payment>> getStudentPayments(@PathVariable Long studentId) {
        return ResponseEntity.ok(paymentService.getPaymentsByStudent(studentId));
    }

    // ==================== NOTIFICATION ENDPOINTS ====================
    
    // Get unread notifications for student
    @GetMapping("/notifications/{studentId}")
    public ResponseEntity<List<com.team.revaluation.model.Notification>> getUnreadNotifications(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(paymentService.getUnreadNotifications(studentId));
    }

    // Mark notification as read
    @PutMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markNotificationRead(@PathVariable Long notificationId) {
        paymentService.markNotificationAsRead(notificationId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        return ResponseEntity.ok(response);
    }
}