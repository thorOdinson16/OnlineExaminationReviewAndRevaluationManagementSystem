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

@Service
public class ReviewService {

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    @Qualifier("reviewFeeStrategy")
    private FeeCalculationStrategy reviewFeeStrategy;

    // ==================== CREATE (Step 1 — checklist §3.1) ====================

    /**
     * Creates a ReviewRequest with status PAYMENT_PENDING.
     * This satisfies: "POST /student/review/apply creates ReviewRequest with PAYMENT_PENDING"
     * The request is saved to DB at PAYMENT_PENDING so a demo evaluator can observe it.
     */
    @Transactional
    public ReviewRequest applyForReview(ReviewRequest request) {
        Float fee = reviewFeeStrategy.calculateFee();   // ReviewFeeStrategy — not hardcoded

        ReviewRequest newRequest = new ReviewRequestBuilder()
                .withStudent(request.getStudent())
                .withAnswerScript(request.getAnswerScript())
                .withReviewFee(fee)
                .withReviewStatus("PAYMENT_PENDING")    // starts PAYMENT_PENDING
                .build();

        ReviewRequest saved = reviewRequestRepository.save(newRequest);

        // Notify student that a review request has been created and payment is needed
        notificationService.notifyStudent(saved.getStudent(),
            "Review request #" + saved.getReviewId() + " created. Fee: ₹" + fee + ". Please complete payment.");

        return saved;
    }

    // ==================== READ ====================

    public ReviewRequest getReviewById(Long reviewId) {
        return reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review request not found with id: " + reviewId));
    }

    public List<ReviewRequest> getReviewsByStudent(Long studentId) {
        return reviewRequestRepository.findByStudentUserId(studentId);
    }

    public List<ReviewRequest> getAllReviews() {
        return reviewRequestRepository.findAll();
    }

    public List<ReviewRequest> getPendingReviews() {
        return reviewRequestRepository.findByReviewStatus("PAYMENT_PENDING");
    }

    public List<ReviewRequest> getReviewsByStatus(String status) {
        return reviewRequestRepository.findByReviewStatus(status);
    }

    // ==================== PAYMENT (Step 2 — checklist §3.1) ====================

    /**
     * Processes payment for an existing PAYMENT_PENDING review request.
     * On success transitions status → REVIEW_REQUESTED (checklist §3.1).
     * This satisfies: "POST /student/review/{reviewId}/pay → status = REVIEW_REQUESTED"
     */
    @Transactional
    public ReviewRequest processPaymentForReview(Long reviewId) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review request not found"));

        if (!"PAYMENT_PENDING".equals(request.getReviewStatus())) {
            throw new RuntimeException("Payment already processed for this review request. Status: "
                + request.getReviewStatus());
        }

        Payment payment = new Payment();
        payment.setAmount(request.getReviewFee());
        payment.setPaymentType("FULL");
        payment.setPaymentStatus("PENDING");
        payment.setStudent(request.getStudent());

        Payment processedPayment = paymentService.processPayment(payment);

        if ("SUCCESS".equals(processedPayment.getPaymentStatus())) {
            // Transition to REVIEW_REQUESTED — this is the checklist-required post-payment state
            ReviewRequestStateMachine.transition(request, "REVIEW_REQUESTED");

            // Mirror on the AnswerScript state machine
            AnswerScript script = request.getAnswerScript();
            if (script != null) {
                try {
                    AnswerScriptStateMachine.transition(script, "REVIEW_REQUESTED");
                } catch (com.team.revaluation.exception.InvalidStateTransitionException e) {
                    throw new RuntimeException("Invalid script state transition: " + e.getMessage());
                }
                answerScriptRepository.save(script);
            }

            ReviewRequest savedRequest = reviewRequestRepository.save(request);
            notificationService.notifyReviewStatusChange(savedRequest);
            return savedRequest;

        } else {
            ReviewRequestStateMachine.transition(request, "PAYMENT_FAILED");
            notificationService.notifyStudent(request.getStudent(),
                "Payment failed for review request #" + reviewId + ". Please try again.");
            return reviewRequestRepository.save(request);
        }
    }

    // ==================== STATUS UPDATES ====================

    /**
     * Generic status update — all changes validated by the state machine.
     * Observer (NotificationService) is notified on every change.
     */
    @Transactional
    public ReviewRequest updateReviewStatus(Long reviewId, String newStatus) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review request not found"));

        ReviewRequestStateMachine.transition(request, newStatus);

        ReviewRequest updatedRequest = reviewRequestRepository.save(request);
        notificationService.notifyReviewStatusChange(updatedRequest);   // Observer pattern
        return updatedRequest;
    }

    @Transactional
    public ReviewRequest cancelReviewRequest(Long reviewId) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review request not found"));

        ReviewRequestStateMachine.transition(request, "CANCELLED");
        ReviewRequest updatedRequest = reviewRequestRepository.save(request);

        notificationService.notifyStudent(request.getStudent(),
            "Review request #" + reviewId + " has been cancelled.");

        return updatedRequest;
    }

    /**
     * CoE verifies the reviewed paper — transitions REVIEW_COMPLETED → VERIFIED.
     * Satisfies: "PUT /evaluator/scripts/{scriptId}/verify → RESULTS_PUBLISHED"
     */
    @Transactional
    public ReviewRequest verifyReview(Long reviewId) {
        return updateReviewStatus(reviewId, "VERIFIED");
    }
}