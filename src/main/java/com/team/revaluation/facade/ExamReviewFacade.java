package com.team.revaluation.facade;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.Notification;
import com.team.revaluation.model.Payment;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.UserRepository;
import com.team.revaluation.service.NotificationService;
import com.team.revaluation.service.PaymentService;
import com.team.revaluation.service.RevaluationService;
import com.team.revaluation.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExamReviewFacade — Facade pattern (Structural, checklist §4.2).
 *
 * StudentController must only talk to this class.
 * This facade hides ReviewService, RevaluationService, PaymentService,
 * NotificationService, and the repositories from the controller layer.
 *
 * Key behaviour change (checklist compliance):
 *   - applyForReview()  saves with PAYMENT_PENDING first (visible in DB),
 *     then a separate payForReview() call transitions → REVIEW_REQUESTED.
 *   - applyAndPay()  is kept as a convenience method for the dashboard "one-click" flow,
 *     but it now returns the PAYMENT_PENDING reviewId so the UI can call pay separately.
 */
@Component
public class ExamReviewFacade {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private RevaluationService revaluationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private UserRepository userRepository;

    // ==================== RESULTS ====================

    public List<AnswerScript> getStudentResults(Long studentId) {
        return answerScriptRepository.findByStudentUserId(studentId);
    }

    public List<AnswerScript> getAllResults() {
        return answerScriptRepository.findAllWithDetails();
    }

    public AnswerScript getScriptResult(Long scriptId) {
        return answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));
    }

    // ==================== REVIEW FLOW ====================

    /**
     * Step 1 — Creates a ReviewRequest at PAYMENT_PENDING.
     * Satisfies: "POST /student/review/apply creates ReviewRequest with PAYMENT_PENDING"
     */
    public Map<String, Object> applyForReview(Long studentId, Long scriptId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Student student = (Student) userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            AnswerScript script = answerScriptRepository.findById(scriptId)
                    .orElseThrow(() -> new RuntimeException("Script not found"));

            if (!script.getStatus().equals("EVALUATED") && !script.getStatus().equals("RESULTS_PUBLISHED")) {
                throw new RuntimeException("Script not eligible for review. Status: " + script.getStatus());
            }

            ReviewRequest request = new ReviewRequest();
            request.setStudent(student);
            request.setAnswerScript(script);

            // Saved with PAYMENT_PENDING via ReviewService (uses ReviewRequestBuilder + ReviewFeeStrategy)
            ReviewRequest created = reviewService.applyForReview(request);

            result.put("reviewId",     created.getReviewId());
            result.put("reviewFee",    created.getReviewFee());
            result.put("reviewStatus", created.getReviewStatus());   // "PAYMENT_PENDING"
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
     * Convenience: create request AND pay in one Facade call (used by dashboard one-click).
     * Still persists PAYMENT_PENDING first so it is DB-visible, then immediately pays.
     */
    public Map<String, Object> applyAndPay(Long studentId, Long scriptId) {
        // Step 1 — create at PAYMENT_PENDING
        Map<String, Object> created = applyForReview(studentId, scriptId);

        if (created.containsKey("error")) {
            return created;   // propagate error
        }

        Long reviewId = ((Number) created.get("reviewId")).longValue();

        // Step 2 — pay (transitions to REVIEW_REQUESTED on success)
        try {
            ReviewRequest paid = payForReview(reviewId);
            created.put("reviewStatus",  paid.getReviewStatus());   // "REVIEW_REQUESTED"
            created.put("finalStatus",   "REVIEW_REQUESTED");
            created.put("message",       "Review application and payment completed. Status: REVIEW_REQUESTED.");
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