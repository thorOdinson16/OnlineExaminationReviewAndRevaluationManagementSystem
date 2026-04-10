package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.ReviewRequestRepository;
import com.team.revaluation.repository.RevaluationRequestRepository;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chain of Responsibility — link 3 of 4.
 *
 * Validates that the student has an active PAYMENT_PENDING review or revaluation
 * request before allowing the payment to proceed.
 *
 * FIX: Previously only logged a warning and passed through even when no eligible
 * request existed, making the handler a no-op guard.  Now it throws, so the chain
 * actually blocks invalid payments as required by checklist §3.4.
 */
@Component
public class ScriptStatusValidationHandler extends PaymentValidationHandler {

    private AnswerScriptRepository answerScriptRepository;
    private ReviewRequestRepository reviewRequestRepository;
    private RevaluationRequestRepository revaluationRequestRepository;

    @Autowired
    public void setAnswerScriptRepository(AnswerScriptRepository repo) {
        this.answerScriptRepository = repo;
    }

    @Autowired
    public void setReviewRequestRepository(ReviewRequestRepository repo) {
        this.reviewRequestRepository = repo;
    }

    @Autowired
    public void setRevaluationRequestRepository(RevaluationRequestRepository repo) {
        this.revaluationRequestRepository = repo;
    }

    @Override
    public void handle(Payment payment, UserRepository userRepository) {
        System.out.println("ScriptStatusValidationHandler: Validating script eligibility");

        if (payment.getStudent() == null || payment.getStudent().getUserId() == null) {
            // Student guard already handled by StudentExistsValidationHandler upstream;
            // pass through here so we don't duplicate that error message.
            handleNext(payment, userRepository);
            return;
        }

        Long studentId = payment.getStudent().getUserId();
        String paymentType = payment.getPaymentType();

        if ("PARTIAL".equalsIgnoreCase(paymentType)) {
            // Review payment: student must have a PAYMENT_PENDING review request
            List<ReviewRequest> reviews = reviewRequestRepository.findByStudentUserId(studentId);
            boolean hasEligibleReview = reviews.stream()
                    .anyMatch(r -> "PAYMENT_PENDING".equals(r.getReviewStatus()));

            if (!hasEligibleReview) {
                throw new RuntimeException(
                    "No PAYMENT_PENDING review request found for student ID " + studentId +
                    ". Cannot process partial payment.");
            }
            System.out.println("ScriptStatusValidationHandler: Valid PAYMENT_PENDING review request found.");

        } else if ("FULL".equalsIgnoreCase(paymentType)) {
            // Revaluation payment: student must have a PAYMENT_PENDING revaluation request
            List<RevaluationRequest> revaluations =
                    revaluationRequestRepository.findByStudentUserId(studentId);
            boolean hasEligibleRevaluation = revaluations.stream()
                    .anyMatch(r -> "PAYMENT_PENDING".equals(r.getRevaluationStatus()));

            if (!hasEligibleRevaluation) {
                throw new RuntimeException(
                    "No PAYMENT_PENDING revaluation request found for student ID " + studentId +
                    ". Cannot process full payment.");
            }
            System.out.println("ScriptStatusValidationHandler: Valid PAYMENT_PENDING revaluation request found.");

        } else {
            // Unknown payment type — fail safe
            throw new RuntimeException(
                "Unknown payment type '" + paymentType + "'. Expected PARTIAL or FULL.");
        }

        handleNext(payment, userRepository);
    }
}