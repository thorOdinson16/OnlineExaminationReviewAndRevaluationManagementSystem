package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.ReviewRequest;

import java.util.HashSet;
import java.util.Set;

/**
 * State Machine for ReviewRequest.
 *
 * Valid States (aligned with checklist §3.1 and §6):
 *   PAYMENT_PENDING    — review request created, awaiting fee payment
 *   REVIEW_REQUESTED   — fee paid successfully; request submitted to CoE  ← checklist-required name
 *   PAYMENT_FAILED     — payment attempt failed
 *   UNDER_REVIEW       — CoE assigned evaluator; evaluator reviewing
 *   REVIEW_COMPLETED   — evaluator finished; paper visible to student
 *   VERIFIED           — CoE verified the reviewed paper
 *   REJECTED           — CoE rejected the request
 *   CANCELLED          — student cancelled before payment
 *
 * NOTE: "REVIEW_REQUESTED" replaces the old "PAYMENT_SUCCESS" label so that the
 * checklist item "POST /student/review/{reviewId}/pay → status = REVIEW_REQUESTED"
 * is satisfied exactly.
 */
public class ReviewRequestStateMachine {

    private static final Set<String> ALLOWED_TRANSITIONS = new HashSet<>();

    static {
        // Payment flow
        addTransition("PAYMENT_PENDING",  "REVIEW_REQUESTED");   // payment succeeded
        addTransition("PAYMENT_PENDING",  "PAYMENT_FAILED");
        addTransition("PAYMENT_PENDING",  "CANCELLED");

        // Retry after failure
        addTransition("PAYMENT_FAILED",   "PAYMENT_PENDING");
        addTransition("PAYMENT_FAILED",   "CANCELLED");

        // Admin / CoE actions after student requests review
        addTransition("REVIEW_REQUESTED", "UNDER_REVIEW");        // CoE assigns evaluator
        addTransition("REVIEW_REQUESTED", "VERIFIED");
        addTransition("REVIEW_REQUESTED", "REJECTED");

        // Evaluator finishes
        addTransition("UNDER_REVIEW",     "REVIEW_COMPLETED");

        // CoE verifies after review is complete
        addTransition("REVIEW_COMPLETED", "VERIFIED");

        // Terminal states: VERIFIED, REJECTED, CANCELLED have no outgoing transitions
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

    /** Check without throwing — useful for guard conditions in the service layer. */
    public static boolean isTransitionAllowed(String current, String newStatus) {
        if (current == null) current = "PAYMENT_PENDING";
        return ALLOWED_TRANSITIONS.contains(current + "->" + newStatus);
    }
}