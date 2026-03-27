package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.RevaluationRequest;

import java.util.HashSet;
import java.util.Set;

public class RevaluationRequestStateMachine {

    private static final Set<String> ALLOWED_TRANSITIONS = new HashSet<>();

    static {
        // Payment flow
        addTransition("PAYMENT_PENDING", "PAYMENT_SUCCESS");
        addTransition("PAYMENT_PENDING", "PAYMENT_FAILED");
        addTransition("PAYMENT_PENDING", "CANCELLED");
        
        // Admin verification flow
        addTransition("PAYMENT_SUCCESS", "VERIFIED");
        addTransition("PAYMENT_SUCCESS", "REJECTED");
        addTransition("PAYMENT_FAILED", "PAYMENT_PENDING");
        addTransition("PAYMENT_FAILED", "CANCELLED");
        
        // Revaluation flow
        addTransition("VERIFIED", "REVALUATION_IN_PROGRESS");
        addTransition("REVALUATION_IN_PROGRESS", "REVALUATION_COMPLETED");
        
        // Terminal states
        // REVALUATION_COMPLETED, CANCELLED, REJECTED have no outgoing transitions
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