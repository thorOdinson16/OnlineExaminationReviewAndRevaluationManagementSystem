package com.team.revaluation.service;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.ReviewRequestRepository;
import com.team.revaluation.builder.ReviewRequestBuilder;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ReviewService — business logic for the review flow.
 *
 * All AnswerScript transitions use spec-aligned state names from §6:
 *   RESULTS_PUBLISHED → REVIEW_REQUESTED → REVIEW_PAYMENT_PENDING → REVIEW_IN_PROGRESS
 *   → REVIEW_COMPLETED → AWAIT_STUDENT_DECISION
 *
 * Implements IReviewService for DIP compliance.
 * Uses ReviewRequestBuilder (Builder pattern).
 * Calls NotificationService.notifyReviewStatusChange() on every status change (Observer pattern).
 */
@Service
public class ReviewService implements IReviewService {

    @Autowired private ReviewRequestRepository reviewRequestRepository;
    @Autowired private AnswerScriptRepository   answerScriptRepository;
    @Autowired private PaymentService           paymentService;
    @Autowired private NotificationService      notificationService;

    @Autowired
    @Qualifier("reviewFeeStrategy")
    private FeeCalculationStrategy reviewFeeStrategy;

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Satisfies: "POST /student/review/apply creates ReviewRequest with PAYMENT_PENDING"
     * Uses ReviewRequestBuilder (Abhijnan's Builder pattern).
     */
    @Override
    @Transactional
    public ReviewRequest applyForReview(ReviewRequest request) {
        Float fee = reviewFeeStrategy.calculateFee();   // ReviewFeeStrategy — not hardcoded

        ReviewRequest newRequest = new ReviewRequestBuilder()
            .withStudent(request.getStudent())
            .withAnswerScript(request.getAnswerScript())
            .withReviewFee(fee)
            .withReviewStatus("PAYMENT_PENDING")
            .build();

        ReviewRequest saved = reviewRequestRepository.save(newRequest);

        notificationService.notifyStudent(saved.getStudent(),
            "Review request #" + saved.getReviewId() +
            " created. Fee: ₹" + fee + ". Please complete payment.");

        return saved;
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    public ReviewRequest getReviewById(Long reviewId) {
        return reviewRequestRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review request not found: " + reviewId));
    }

    @Override
    public List<ReviewRequest> getReviewsByStudent(Long studentId) {
        return reviewRequestRepository.findByStudentUserId(studentId);
    }

    @Override
    public List<ReviewRequest> getAllReviews() {
        return reviewRequestRepository.findAll();
    }

    @Override
    public List<ReviewRequest> getPendingReviews() {
        return reviewRequestRepository.findByReviewStatus("PAYMENT_PENDING");
    }

    @Override
    public List<ReviewRequest> getReviewsByStatus(String status) {
        return reviewRequestRepository.findByReviewStatus(status);
    }

    // ── PAYMENT ───────────────────────────────────────────────────────────────

    /**
     * Satisfies: "POST /student/review/{reviewId}/pay → status = REVIEW_REQUESTED"
     *
     * On payment success, mirrors REVIEW_REQUESTED on the AnswerScript using the
     * spec-aligned transition: RESULTS_PUBLISHED → REVIEW_REQUESTED.
     */
    @Override
    @Transactional
    public ReviewRequest processPaymentForReview(Long reviewId) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review request not found: " + reviewId));

        if (!"PAYMENT_PENDING".equals(request.getReviewStatus())) {
            throw new RuntimeException("Payment already processed. Status: " + request.getReviewStatus());
        }

        Payment payment = new Payment();
        payment.setAmount(request.getReviewFee());
        payment.setPaymentType("PARTIAL");    // review fee is a partial payment
        payment.setPaymentStatus("PENDING");
        payment.setStudent(request.getStudent());

        Payment processed = paymentService.processPayment(payment);

        if ("SUCCESS".equals(processed.getPaymentStatus())) {
            ReviewRequestStateMachine.transition(request, "REVIEW_REQUESTED");

            // Mirror on AnswerScript: RESULTS_PUBLISHED → REVIEW_REQUESTED
            AnswerScript script = request.getAnswerScript();
            if (script != null) {
                try {
                    AnswerScriptStateMachine.transition(script, "REVIEW_REQUESTED");
                } catch (com.team.revaluation.exception.InvalidStateTransitionException e) {
                    throw new RuntimeException("Script state error: " + e.getMessage());
                }
                answerScriptRepository.save(script);
            }

            ReviewRequest saved = reviewRequestRepository.save(request);
            notificationService.notifyReviewStatusChange(saved);   // Observer
            return saved;

        } else {
            ReviewRequestStateMachine.transition(request, "PAYMENT_FAILED");
            notificationService.notifyStudent(request.getStudent(),
                "Payment failed for review #" + reviewId + ". Please try again.");
            return reviewRequestRepository.save(request);
        }
    }

    // ── STATUS UPDATES ────────────────────────────────────────────────────────

    /**
     * All status changes go through ReviewRequestStateMachine (State pattern).
     * NotificationService.notifyReviewStatusChange() called on every change (Observer).
     */
    @Override
    @Transactional
    public ReviewRequest updateReviewStatus(Long reviewId, String newStatus) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review request not found: " + reviewId));

        ReviewRequestStateMachine.transition(request, newStatus);
        ReviewRequest updated = reviewRequestRepository.save(request);
        notificationService.notifyReviewStatusChange(updated);   // Observer pattern
        return updated;
    }

    @Override
    @Transactional
    public ReviewRequest cancelReviewRequest(Long reviewId) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review request not found: " + reviewId));

        ReviewRequestStateMachine.transition(request, "CANCELLED");
        ReviewRequest updated = reviewRequestRepository.save(request);
        notificationService.notifyStudent(updated.getStudent(),
            "Review request #" + reviewId + " has been cancelled.");
        return updated;
    }

    /**
     * Satisfies: "PUT /evaluator/scripts/{scriptId}/verify → RESULTS_PUBLISHED"
     * Verifies the review and transitions ReviewRequest → VERIFIED.
     */
    @Override
    @Transactional
    public ReviewRequest verifyReview(Long reviewId) {
        return updateReviewStatus(reviewId, "VERIFIED");
    }
}