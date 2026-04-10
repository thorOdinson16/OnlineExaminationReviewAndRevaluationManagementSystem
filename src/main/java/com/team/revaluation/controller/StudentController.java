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
 * StudentController — thin controller, Facade pattern (checklist §4.2).
 *
 * This controller only talks to ExamReviewFacade.
 * No service, repository, or state-machine class is imported here.
 *
 * Checklist §3.1 endpoint mapping:
 *   GET  /student/results/{studentId}         → returns AnswerScript list
 *   POST /student/review/apply                → creates ReviewRequest with PAYMENT_PENDING
 *   POST /student/review/{reviewId}/pay       → calls PaymentService → status = REVIEW_REQUESTED
 *   POST /student/revaluation/apply           → RevaluationRequest with PAYMENT_PENDING
 *   POST /student/revaluation/{id}/pay        → calls PaymentService → REVALUATION_IN_PROGRESS
 */
@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private ExamReviewFacade examReviewFacade;

    // ==================== RESULTS ====================

    /** Satisfies: "GET /student/results/{studentId} returns AnswerScript list" */
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

    // ==================== REVIEW FLOW — two explicit steps ====================

    /**
     * Step 1: creates ReviewRequest with status PAYMENT_PENDING and persists it.
     * Satisfies: "POST /student/review/apply creates ReviewRequest with PAYMENT_PENDING"
     */
    @PostMapping("/review/apply")
    public ResponseEntity<Map<String, Object>> applyForReview(@RequestBody Map<String, Object> request) {
        try {
            Long studentId = extractLong(request, "studentId", "student", "userId");
            Long scriptId  = extractLong(request, "scriptId",  "answerScript", "scriptId");

            if (studentId == null || scriptId == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error",   "Missing required fields");
                error.put("message", "studentId and scriptId are required");
                return ResponseEntity.badRequest().body(error);
            }

            // Facade.applyForReview saves at PAYMENT_PENDING — no payment yet
            Map<String, Object> result = examReviewFacade.applyForReview(studentId, scriptId);

            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error",   e.getMessage());
            error.put("message", "Failed to apply for review: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Step 2: processes payment for an existing PAYMENT_PENDING review request.
     * Transitions: PAYMENT_PENDING → REVIEW_REQUESTED (via ReviewRequestStateMachine).
     * Satisfies: "POST /student/review/{reviewId}/pay → status = REVIEW_REQUESTED"
     */
    @PostMapping("/review/{reviewId}/pay")
    public ResponseEntity<?> payForReview(@PathVariable Long reviewId) {
        try {
            ReviewRequest paid = examReviewFacade.payForReview(reviewId);
            return ResponseEntity.ok(paid);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error",   e.getMessage());
            error.put("message", "Payment failed: " + e.getMessage());
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

    @DeleteMapping("/review/{reviewId}/cancel")
    public ResponseEntity<Map<String, String>> cancelReview(@PathVariable Long reviewId) {
        examReviewFacade.cancelReview(reviewId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Review request cancelled successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== REVALUATION FLOW ====================

    /**
     * Step 1: creates RevaluationRequest with status PAYMENT_PENDING.
     * Satisfies: "POST /student/revaluation/apply — RevaluationRequest with PAYMENT_PENDING"
     */
    @PostMapping("/revaluation/apply")
    public ResponseEntity<?> applyForRevaluation(
            @RequestParam Long scriptId,
            @RequestParam Long studentId) {
        try {
            RevaluationRequest created = examReviewFacade.applyForRevaluation(scriptId, studentId);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error",   e.getMessage());
            error.put("message", "Failed to apply for revaluation: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Step 2: processes full fee payment.
     * Transitions: PAYMENT_PENDING → REVALUATION_IN_PROGRESS.
     * Satisfies: "POST /student/revaluation/{id}/pay — calls PaymentService → REVALUATION_IN_PROGRESS"
     */
    @PostMapping("/revaluation/{revaluationId}/pay")
    public ResponseEntity<?> payForRevaluation(@PathVariable Long revaluationId) {
        try {
            RevaluationRequest paid = examReviewFacade.payForRevaluation(revaluationId);
            return ResponseEntity.ok(paid);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error",   e.getMessage());
            error.put("message", "Payment failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/revaluation/{revaluationId}")
    public ResponseEntity<RevaluationRequest> getRevaluationById(@PathVariable Long revaluationId) {
        return ResponseEntity.ok(examReviewFacade.getRevaluationById(revaluationId));
    }

    @GetMapping("/revaluation/student/{studentId}")
    public ResponseEntity<List<RevaluationRequest>> getMyRevaluations(@PathVariable Long studentId) {
        return ResponseEntity.ok(examReviewFacade.getMyRevaluations(studentId));
    }

    @DeleteMapping("/revaluation/{revaluationId}/cancel")
    public ResponseEntity<Map<String, String>> cancelRevaluation(@PathVariable Long revaluationId) {
        examReviewFacade.cancelRevaluation(revaluationId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Revaluation request cancelled successfully");
        return ResponseEntity.ok(response);
    }

    // ==================== PAYMENT INFO ====================

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

    @SuppressWarnings("unchecked")
    private Long extractLong(Map<String, Object> body, String flatKey, String nestedKey, String nestedField) {
        if (body.get(flatKey) instanceof Number) {
            return ((Number) body.get(flatKey)).longValue();
        }
        if (body.get(nestedKey) instanceof Map) {
            Map<String, Object> nested = (Map<String, Object>) body.get(nestedKey);
            if (nested.get(nestedField) instanceof Number) {
                return ((Number) nested.get(nestedField)).longValue();
            }
        }
        return null;
    }
}