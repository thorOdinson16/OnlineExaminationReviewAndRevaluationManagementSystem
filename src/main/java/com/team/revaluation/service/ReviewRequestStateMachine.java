package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.ReviewRequest;

import java.util.HashSet;
import java.util.Set;

/**
 * State Machine for ReviewRequest.
 *
 * Valid States:
 *   PAYMENT_PENDING  — review request created, awaiting fee payment
 *   PAYMENT_SUCCESS  — fee paid, request submitted to CoE
 *   PAYMENT_FAILED   — payment attempt failed
 *   IN_PROGRESS      — evaluator assigned and reviewing
 *   COMPLETED        — review done, result visible to student
 *   VERIFIED         — CoE has verified the reviewed paper
 *   REJECTED         — CoE rejected the request
 *   CANCELLED        — student cancelled before payment
 *
 * All calls to ReviewRequest.setReviewStatus() must go through
 * ReviewRequestStateMachine.transition() — never call setReviewStatus() directly.
 */
public class ReviewRequestStateMachine {

    private static final Set<String> ALLOWED_TRANSITIONS = new HashSet<>();

    static {
        // Payment flow
        addTransition("PAYMENT_PENDING", "PAYMENT_SUCCESS");
        addTransition("PAYMENT_PENDING", "PAYMENT_FAILED");
        addTransition("PAYMENT_PENDING", "CANCELLED");

        // Retry after failure
        addTransition("PAYMENT_FAILED", "PAYMENT_PENDING");
        addTransition("PAYMENT_FAILED", "CANCELLED");

        // Admin/CoE actions after successful payment
        addTransition("PAYMENT_SUCCESS", "IN_PROGRESS");
        addTransition("PAYMENT_SUCCESS", "VERIFIED");
        addTransition("PAYMENT_SUCCESS", "REJECTED");

        // Re-open for evaluation if needed
        addTransition("VERIFIED", "IN_PROGRESS");

        // Evaluator completes review
        addTransition("IN_PROGRESS", "COMPLETED");

        // Terminal states: COMPLETED, REJECTED, CANCELLED have no outgoing transitions
    }

    private static void addTransition(String from, String to) {
        ALLOWED_TRANSITIONS.add(from + "->" + to);
    }

    /**
     * Validates and performs a status transition on the given ReviewRequest.
     *
     * @param request   the review request whose status will be changed
     * @param newStatus the desired new status
     * @throws InvalidStateTransitionException if the transition is not allowed
     */
    public static void transition(ReviewRequest request, String newStatus) {
        String current = request.getReviewStatus();
        if (current == null) {
            current = "PAYMENT_PENDING";
            request.setReviewStatus(current);
        }
        if (current.equals(newStatus)) {
            return; // already in target state — no-op
        }
        String key = current + "->" + newStatus;
        if (!ALLOWED_TRANSITIONS.contains(key)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid review state transition from '%s' to '%s'", current, newStatus)
            );
        }
        request.setReviewStatus(newStatus);
    }

    /**
     * Check without throwing — useful for guard conditions in service layer.
     */
    public static boolean isTransitionAllowed(String current, String newStatus) {
        if (current == null) current = "PAYMENT_PENDING";
        return ALLOWED_TRANSITIONS.contains(current + "->" + newStatus);
    }
}