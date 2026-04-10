package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.RevaluationRequest;

import java.util.HashSet;
import java.util.Set;

/**
 * State Machine for RevaluationRequest.
 *
 * Valid States (aligned with checklist §3.3 and §6):
 *   PAYMENT_PENDING          — revaluation request created, awaiting fee payment
 *   REVALUATION_IN_PROGRESS  — full fee paid; revaluator assigned and working  ← checklist-required post-payment state
 *   PAYMENT_FAILED           — payment attempt failed
 *   REVALUATION_COMPLETED    — revaluator submitted new marks
 *   CANCELLED                — student cancelled
 *   REJECTED                 — CoE rejected the request
 *
 * NOTE: The checklist (§3.3) says:
 *   "POST /student/revaluation/{id}/pay — calls PaymentService → REVALUATION_IN_PROGRESS"
 * So payment success transitions directly to REVALUATION_IN_PROGRESS (admin assigns
 * revaluator within the same flow via AdminController, not as a separate state gate).
 */
public class RevaluationRequestStateMachine {

    private static final Set<String> ALLOWED_TRANSITIONS = new HashSet<>();

    static {
        // Payment flow — payment success goes straight to REVALUATION_IN_PROGRESS (checklist §3.3)
        addTransition("PAYMENT_PENDING",         "REVALUATION_IN_PROGRESS");
        addTransition("PAYMENT_PENDING",         "PAYMENT_FAILED");
        addTransition("PAYMENT_PENDING",         "CANCELLED");

        // Retry after failure
        addTransition("PAYMENT_FAILED",          "PAYMENT_PENDING");
        addTransition("PAYMENT_FAILED",          "CANCELLED");

        // Revaluator submits marks
        addTransition("REVALUATION_IN_PROGRESS", "REVALUATION_COMPLETED");

        // Admin can reject before revaluator is assigned
        addTransition("REVALUATION_IN_PROGRESS", "REJECTED");

        // Terminal states: REVALUATION_COMPLETED, CANCELLED, REJECTED have no outgoing transitions
    }

    private static void addTransition(String from, String to) {
        ALLOWED_TRANSITIONS.add(from + "->" + to);
    }

    public static void transition(RevaluationRequest request, String newStatus) {
        String current = request.getRevaluationStatus();
        if (current == null) {
            current = "PAYMENT_PENDING";
            request.setRevaluationStatus(current);
        }
        if (current.equals(newStatus)) {
            return;
        }
        String transitionKey = current + "->" + newStatus;
        if (!ALLOWED_TRANSITIONS.contains(transitionKey)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid revaluation state transition from '%s' to '%s'", current, newStatus)
            );
        }
        request.setRevaluationStatus(newStatus);
    }

    public static boolean isTransitionAllowed(String currentStatus, String newStatus) {
        if (currentStatus == null) {
            currentStatus = "PAYMENT_PENDING";
        }
        return ALLOWED_TRANSITIONS.contains(currentStatus + "->" + newStatus);
    }
}