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
 * ExamReviewFacade — Facade pattern.
 *
 * StudentController must only talk to this class.
 * This facade hides ReviewService, RevaluationService, PaymentService,
 * NotificationService, and the repositories from the controller layer.
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
     * Creates a ReviewRequest (PAYMENT_PENDING) and immediately processes payment.
     * This is the primary Facade operation — wraps Review + Payment + Notification
     * in a single call so the controller stays thin.
     */
    public Map<String, Object> applyAndPay(Long studentId, Long scriptId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Student student = (Student) userRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Student not found"));

            AnswerScript script = answerScriptRepository.findById(scriptId)
                    .orElseThrow(() -> new RuntimeException("Script not found"));

            if (!script.getStatus().equals("EVALUATED") && !script.getStatus().equals("RESULTS_PUBLISHED")) {
                throw new RuntimeException("Script is not eligible for review. Current status: " + script.getStatus());
            }

            // Step 1 — create ReviewRequest with PAYMENT_PENDING via ReviewService
            ReviewRequest request = new ReviewRequest();
            request.setStudent(student);
            request.setAnswerScript(script);
            ReviewRequest created = reviewService.applyForReview(request);

            result.put("reviewId", created.getReviewId());
            result.put("reviewFee", created.getReviewFee());
            result.put("reviewStatus", created.getReviewStatus());

            // Step 2 — process payment
            Payment payment = new Payment();
            payment.setAmount(created.getReviewFee());
            payment.setPaymentType("FULL");
            payment.setPaymentStatus("PENDING");
            payment.setStudent(student);
            Payment processed = paymentService.processPayment(payment);

            result.put("paymentId", processed.getPaymentId());
            result.put("paymentStatus", processed.getPaymentStatus());

            // Step 3 — update review status based on payment outcome
            if ("SUCCESS".equals(processed.getPaymentStatus())) {
                ReviewRequest updated = reviewService.processPaymentForReview(created.getReviewId());
                result.put("reviewStatus", updated.getReviewStatus());
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

    public ReviewRequest getReviewById(Long reviewId) {
        return reviewService.getReviewById(reviewId);
    }

    public List<ReviewRequest> getMyReviews(Long studentId) {
        return reviewService.getReviewsByStudent(studentId);
    }

    public ReviewRequest payForReview(Long reviewId) {
        return reviewService.processPaymentForReview(reviewId);
    }

    public void cancelReview(Long reviewId) {
        reviewService.cancelReviewRequest(reviewId);
    }

    // ==================== REVALUATION FLOW ====================

    public RevaluationRequest applyForRevaluation(Long scriptId, Long studentId) {
        return revaluationService.applyForRevaluation(scriptId, studentId);
    }

    public RevaluationRequest getRevaluationById(Long revaluationId) {
        return revaluationService.getRevaluationById(revaluationId);
    }

    public List<RevaluationRequest> getMyRevaluations(Long studentId) {
        return revaluationService.getRevaluationsByStudent(studentId);
    }

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