package com.team.revaluation.controller;

import com.team.revaluation.facade.ExamReviewFacade;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.Notification;
import com.team.revaluation.model.Payment;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.ReviewRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StudentController — thin controller, Facade pattern.
 *
 * This controller only talks to ExamReviewFacade.
 * No service, repository, or state-machine class is imported here.
 */
@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private ExamReviewFacade examReviewFacade;

    // ==================== RESULTS ====================

    @GetMapping("/results/{studentId}")
    public ResponseEntity<List<AnswerScript>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(examReviewFacade.getStudentResults(studentId));
    }

    @GetMapping("/results/all")
    public ResponseEntity<List<AnswerScript>> getAllResults() {
        return ResponseEntity.ok(examReviewFacade.getAllResults());
    }

    @GetMapping("/results/script/{scriptId}")
    public ResponseEntity<AnswerScript> getScriptResult(@PathVariable Long scriptId) {
        return ResponseEntity.ok(examReviewFacade.getScriptResult(scriptId));
    }

    // ==================== REVIEW FLOW ====================

    /**
     * Apply for paper review.
     * Uses the Facade which internally orchestrates ReviewService + PaymentService + NotificationService.
     */
    @PostMapping("/review/apply")
    public ResponseEntity<Map<String, Object>> applyForReview(@RequestBody Map<String, Object> request) {
        try {
            Long studentId = extractLong(request, "studentId", "student", "userId");
            Long scriptId  = extractLong(request, "scriptId",  "answerScript", "scriptId");

            if (studentId == null || scriptId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Missing required fields");
                error.put("message", "studentId and scriptId are required");
                return ResponseEntity.badRequest().body(error);
            }

            return ResponseEntity.ok(examReviewFacade.applyAndPay(studentId, scriptId));

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("message", "Failed to apply for review: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/review/{reviewId}")
    public ResponseEntity<ReviewRequest> getReviewById(@PathVariable Long reviewId) {
        return ResponseEntity.ok(examReviewFacade.getReviewById(reviewId));
    }

    @GetMapping("/review/student/{studentId}")
    public ResponseEntity<List<ReviewRequest>> getMyReviews(@PathVariable Long studentId) {
        return ResponseEntity.ok(examReviewFacade.getMyReviews(studentId));
    }

    @PostMapping("/review/{reviewId}/pay")
    public ResponseEntity<ReviewRequest> payForReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(examReviewFacade.payForReview(reviewId));
    }

    @DeleteMapping("/review/{reviewId}/cancel")
    public ResponseEntity<Map<String, String>> cancelReview(@PathVariable Long reviewId) {
        examReviewFacade.cancelReview(reviewId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Review request cancelled successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== REVALUATION FLOW ====================

    @PostMapping("/revaluation/apply")
    public ResponseEntity<RevaluationRequest> applyForRevaluation(
            @RequestParam Long scriptId,
            @RequestParam Long studentId) {
        return ResponseEntity.ok(examReviewFacade.applyForRevaluation(scriptId, studentId));
    }

    @GetMapping("/revaluation/{revaluationId}")
    public ResponseEntity<RevaluationRequest> getRevaluationById(@PathVariable Long revaluationId) {
        return ResponseEntity.ok(examReviewFacade.getRevaluationById(revaluationId));
    }

    @GetMapping("/revaluation/student/{studentId}")
    public ResponseEntity<List<RevaluationRequest>> getMyRevaluations(@PathVariable Long studentId) {
        return ResponseEntity.ok(examReviewFacade.getMyRevaluations(studentId));
    }

    @PostMapping("/revaluation/{revaluationId}/pay")
    public ResponseEntity<RevaluationRequest> payForRevaluation(@PathVariable Long revaluationId) {
        return ResponseEntity.ok(examReviewFacade.payForRevaluation(revaluationId));
    }

    @DeleteMapping("/revaluation/{revaluationId}/cancel")
    public ResponseEntity<Map<String, String>> cancelRevaluation(@PathVariable Long revaluationId) {
        examReviewFacade.cancelRevaluation(revaluationId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Revaluation request cancelled successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== PAYMENT ====================

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<Payment> getPaymentStatus(@PathVariable Long paymentId) {
        return ResponseEntity.ok(examReviewFacade.getPaymentById(paymentId));
    }

    @GetMapping("/payment/student/{studentId}")
    public ResponseEntity<List<Payment>> getStudentPayments(@PathVariable Long studentId) {
        return ResponseEntity.ok(examReviewFacade.getStudentPayments(studentId));
    }

    // ==================== NOTIFICATIONS ====================

    @GetMapping("/notifications/{studentId}")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable Long studentId) {
        return ResponseEntity.ok(examReviewFacade.getUnreadNotifications(studentId));
    }

    @PutMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markNotificationRead(@PathVariable Long notificationId) {
        examReviewFacade.markNotificationRead(notificationId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification marked as read");
        return ResponseEntity.ok(response);
    }

    // ==================== HELPER ====================

    /**
     * Extracts a Long value from a flat key or a nested object key in the request body.
     * Handles both {"studentId": 1} and {"student": {"userId": 1}} shapes.
     */
    @SuppressWarnings("unchecked")
    private Long extractLong(Map<String, Object> body, String flatKey, String nestedKey, String nestedField) {
        if (body.get(flatKey) != null) {
            return ((Number) body.get(flatKey)).longValue();
        }
        if (body.get(nestedKey) instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) body.get(nestedKey);
            if (nested.get(nestedField) != null) {
                return ((Number) nested.get(nestedField)).longValue();
            }
        }
        return null;
    }
}