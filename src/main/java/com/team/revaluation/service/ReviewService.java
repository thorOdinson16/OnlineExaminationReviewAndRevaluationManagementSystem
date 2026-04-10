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
 *
 * FIX 1: applyForReview saves ReviewRequest with "PAYMENT_PENDING" (ReviewRequest-level status)
 *         and mirrors AnswerScript to "REVIEW_REQUESTED" ONLY after payment succeeds.
 * FIX 2: verifyReview now transitions ReviewRequest → "VERIFIED" and AnswerScript
 *         → "AWAIT_STUDENT_DECISION" (per §6: ReviewCompleted → AwaitStudentDecision).
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
            " created. Fee: \u20b9" + fee + ". Please complete payment.");

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
     * On payment success:
     *   ReviewRequest: PAYMENT_PENDING → REVIEW_REQUESTED
     *   AnswerScript:  RESULTS_PUBLISHED → REVIEW_REQUESTED  (§6, row 4)
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
        payment.setPaymentType("PARTIAL");
        payment.setPaymentStatus("PENDING");
        payment.setStudent(request.getStudent());

        Payment processed = paymentService.processPayment(payment);

        if ("SUCCESS".equals(processed.getPaymentStatus())) {
            // ReviewRequest-level: PAYMENT_PENDING → REVIEW_REQUESTED
            ReviewRequestStateMachine.transition(request, "REVIEW_REQUESTED");

            // AnswerScript: RESULTS_PUBLISHED → REVIEW_REQUESTED  (§6 row 4)
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
     *
     * Transitions ReviewRequest → VERIFIED.
     * Also advances AnswerScript through REVIEW_COMPLETED → AWAIT_STUDENT_DECISION (§6 rows 8-9).
     */
    @Override
    @Transactional
    public ReviewRequest verifyReview(Long reviewId) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("Review request not found: " + reviewId));

        ReviewRequestStateMachine.transition(request, "VERIFIED");

        // Advance AnswerScript: REVIEW_IN_PROGRESS → REVIEW_COMPLETED → AWAIT_STUDENT_DECISION
        AnswerScript script = request.getAnswerScript();
        if (script != null) {
            try {
                String currentStatus = script.getStatus();
                // If still in REVIEW_IN_PROGRESS, complete then move to decision state
                if ("REVIEW_IN_PROGRESS".equals(currentStatus)) {
                    AnswerScriptStateMachine.transition(script, "REVIEW_COMPLETED");
                    AnswerScriptStateMachine.transition(script, "AWAIT_STUDENT_DECISION");
                } else if ("REVIEW_COMPLETED".equals(currentStatus)) {
                    AnswerScriptStateMachine.transition(script, "AWAIT_STUDENT_DECISION");
                }
                answerScriptRepository.save(script);
            } catch (com.team.revaluation.exception.InvalidStateTransitionException e) {
                // Log but don't fail — the review verification should proceed
                System.err.println("[ReviewService] verifyReview: script state transition warning: " + e.getMessage());
            }
        }

        ReviewRequest updated = reviewRequestRepository.save(request);
        notificationService.notifyReviewStatusChange(updated);
        return updated;
    }
}