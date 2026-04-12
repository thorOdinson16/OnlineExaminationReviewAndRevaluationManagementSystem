package com.team.revaluation.facade;

import com.team.revaluation.model.*;
import com.team.revaluation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExamReviewFacade — Structural: Facade Pattern (Abhijna D S, checklist §4.2).
 *
 * FIX 1 (DIP): Fields changed from concrete ReviewService / RevaluationService to
 *              their interfaces IReviewService / IRevaluationService.
 *              StudentController depends only on this Facade; the Facade depends
 *              only on abstractions — both layers satisfy DIP (checklist §5).
 *
 * FIX 2 (Facade completeness): All student-facing operations are exposed here.
 *              StudentController must NEVER import or call a service directly.
 *
 * Patterns active in this class:
 *   Facade      — wraps IReviewService, IRevaluationService, PaymentService, NotificationService
 *   Observer    — payForReview / payForRevaluation fire NotificationService (via ReviewService)
 *   Strategy    — fee calculation is delegated to ReviewService / RevaluationService
 *   Builder     — ReviewRequestBuilder is used inside ReviewService.applyForReview()
 */
@Component
public class ExamReviewFacade {

    // FIX: depend on interfaces (DIP), not concrete classes
    @Autowired
    private IReviewService reviewService;

    @Autowired
    private IRevaluationService revaluationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private IScriptService scriptService;

    public List<AnswerScript> getStudentResults(Long studentId) {
        return scriptService.getScriptsByStudent(studentId);
    }

    public List<AnswerScript> getAllResults() {
        return scriptService.getAllScripts();
    }

    public AnswerScript getScriptResult(Long scriptId) {
        return scriptService.getScriptById(scriptId);
    }

    // ==================== REVIEW FLOW ====================

    /**
     * Step 1 — Creates a ReviewRequest at PAYMENT_PENDING.
     * Satisfies: "POST /student/review/apply creates ReviewRequest with PAYMENT_PENDING"
     *
     * Internally calls:
     *   ReviewService.applyForReview()   — uses ReviewRequestBuilder + ReviewFeeStrategy
     *   NotificationService.notifyStudent() — Observer pattern
     */
public Map<String, Object> applyForReview(Long studentId, Long scriptId) {
    Map<String, Object> result = new HashMap<>();
    try {
        ReviewRequest request = new ReviewRequest();
        
        Student student = new Student();
        student.setUserId(studentId);
        request.setStudent(student);

        AnswerScript script = new AnswerScript();
        script.setScriptId(scriptId);
        request.setAnswerScript(script);

        ReviewRequest created = reviewService.applyForReview(request);

        result.put("reviewId",     created.getReviewId());
        result.put("reviewFee",    created.getReviewFee());
        result.put("reviewStatus", created.getReviewStatus());
        result.put("message",      "Review request created. Please complete payment.");

    } catch (Exception e) {
        result.put("error",   e.getMessage());
        result.put("message", "Failed to apply for review: " + e.getMessage());
    }
    return result;
}
    /**
     * Step 2 — Processes payment; transitions PAYMENT_PENDING → REVIEW_REQUESTED.
     * Satisfies: "POST /student/review/{reviewId}/pay → status = REVIEW_REQUESTED"
     */
    public ReviewRequest payForReview(Long reviewId) {
        return reviewService.processPaymentForReview(reviewId);
    }

    /**
     * Convenience: create request AND pay in one Facade call.
     * Still persists PAYMENT_PENDING first, then immediately pays.
     */
    public Map<String, Object> applyAndPay(Long studentId, Long scriptId) {
        Map<String, Object> created = applyForReview(studentId, scriptId);
        if (created.containsKey("error")) {
            return created;
        }
        Long reviewId = ((Number) created.get("reviewId")).longValue();
        try {
            ReviewRequest paid = payForReview(reviewId);
            created.put("reviewStatus", paid.getReviewStatus());
            created.put("finalStatus",  "REVIEW_REQUESTED");
            created.put("message",      "Review application and payment completed. Status: REVIEW_REQUESTED.");
        } catch (Exception e) {
            created.put("error",       e.getMessage());
            created.put("finalStatus", "PAYMENT_FAILED");
            created.put("message",     "Payment failed: " + e.getMessage());
        }
        return created;
    }

    public ReviewRequest getReviewById(Long reviewId) {
        return reviewService.getReviewById(reviewId);
    }

    public List<ReviewRequest> getMyReviews(Long studentId) {
        return reviewService.getReviewsByStudent(studentId);
    }

    public void cancelReview(Long reviewId) {
        reviewService.cancelReviewRequest(reviewId);
    }

    // ==================== REVALUATION FLOW ====================

    /**
     * Satisfies: "POST /student/revaluation/apply — RevaluationRequest with PAYMENT_PENDING"
     */
    public RevaluationRequest applyForRevaluation(Long scriptId, Long studentId) {
        return revaluationService.applyForRevaluation(scriptId, studentId);
    }

    public RevaluationRequest getRevaluationById(Long revaluationId) {
        return revaluationService.getRevaluationById(revaluationId);
    }

    public List<RevaluationRequest> getMyRevaluations(Long studentId) {
        return revaluationService.getRevaluationsByStudent(studentId);
    }

    /**
     * Satisfies: "POST /student/revaluation/{id}/pay → REVALUATION_IN_PROGRESS"
     */
    public RevaluationRequest payForRevaluation(Long revaluationId) {
        return revaluationService.processRevaluationPayment(revaluationId);
    }

    public void cancelRevaluation(Long revaluationId) {
        revaluationService.cancelRevaluationRequest(revaluationId);
    }

    // ==================== PAYMENT ====================

    public Payment getPaymentById(Long paymentId) {
        return paymentService.getPaymentById(paymentId);
    }

    public List<Payment> getStudentPayments(Long studentId) {
        return paymentService.getPaymentsByStudent(studentId);
    }

    // ==================== NOTIFICATIONS ====================

    public List<Notification> getUnreadNotifications(Long studentId) {
        return paymentService.getUnreadNotifications(studentId);
    }

    public void markNotificationRead(Long notificationId) {
        paymentService.markNotificationAsRead(notificationId);
    }
}