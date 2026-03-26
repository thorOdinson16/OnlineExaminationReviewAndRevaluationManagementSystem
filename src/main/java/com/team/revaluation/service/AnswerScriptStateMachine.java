package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;

import java.util.HashSet;
import java.util.Set;

public class AnswerScriptStateMachine {

    private static final Set<String> ALLOWED_TRANSITIONS = new HashSet<>();

    static {
        // Define allowed transitions: "current->next"
        
        // Normal evaluation flow
        addTransition("SUBMITTED", "UNDER_EVALUATION");
        addTransition("UNDER_EVALUATION", "EVALUATED");
        addTransition("EVALUATED", "RESULTS_PUBLISHED");
        
        // Review flow with payment states
        addTransition("EVALUATED", "REVIEW_PAYMENT_PENDING");
        addTransition("RESULTS_PUBLISHED", "REVIEW_PAYMENT_PENDING");
        addTransition("REVIEW_PAYMENT_PENDING", "REVIEW_REQUESTED");
        addTransition("REVIEW_PAYMENT_PENDING", "AWAIT_STUDENT_DECISION");
        addTransition("REVIEW_REQUESTED", "REVIEW_IN_PROGRESS");
        addTransition("REVIEW_IN_PROGRESS", "REVIEW_COMPLETED");
        addTransition("REVIEW_COMPLETED", "RESULTS_PUBLISHED");
        
        // Revaluation flow with payment states
        addTransition("EVALUATED", "REVALUATION_PAYMENT_PENDING");
        addTransition("RESULTS_PUBLISHED", "REVALUATION_PAYMENT_PENDING");
        addTransition("REVALUATION_PAYMENT_PENDING", "REVALUATION_REQUESTED");
        addTransition("REVALUATION_PAYMENT_PENDING", "AWAIT_STUDENT_DECISION");
        addTransition("REVALUATION_REQUESTED", "REVALUATION_IN_PROGRESS");
        addTransition("REVALUATION_IN_PROGRESS", "REVALUATION_COMPLETED");
        addTransition("REVALUATION_COMPLETED", "FINALIZED");
        
        // Finalization
        addTransition("RESULTS_PUBLISHED", "FINALIZED");
        addTransition("REVIEW_COMPLETED", "FINALIZED");
        addTransition("REVALUATION_COMPLETED", "FINALIZED");
        
        // Rejection flow
        addTransition("REVIEW_PAYMENT_PENDING", "REJECTED");
        addTransition("REVALUATION_PAYMENT_PENDING", "REJECTED");
        addTransition("REVIEW_REQUESTED", "REJECTED");
        addTransition("REVALUATION_REQUESTED", "REJECTED");
    }

    private static void addTransition(String from, String to) {
        ALLOWED_TRANSITIONS.add(from + "->" + to);
    }

    /**
     * Validates and performs a status transition for the given script.
     *
     * @param script    the answer script whose status will be changed
     * @param newStatus the desired new status
     * @throws InvalidStateTransitionException if the transition is not allowed
     */
    public static void transition(AnswerScript script, String newStatus) {
        String current = script.getStatus();
        if (current == null) {
            current = "SUBMITTED";
            script.setStatus(current);
        }
        if (current.equals(newStatus)) {
            return; // no change
        }
        String transitionKey = current + "->" + newStatus;
        if (!ALLOWED_TRANSITIONS.contains(transitionKey)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid state transition from '%s' to '%s'", current, newStatus)
            );
        }
        script.setStatus(newStatus);
    }
    
    /**
     * Check if a transition is allowed
     */
    public static boolean isTransitionAllowed(String currentStatus, String newStatus) {
        if (currentStatus == null) {
            currentStatus = "SUBMITTED";
        }
        String transitionKey = currentStatus + "->" + newStatus;
        return ALLOWED_TRANSITIONS.contains(transitionKey);
    }
    
    /**
     * Get all allowed next states from a given state
     */
    public static Set<String> getAllowedNextStates(String currentStatus) {
        Set<String> nextStates = new HashSet<>();
        if (currentStatus == null) {
            currentStatus = "SUBMITTED";
        }
        for (String transition : ALLOWED_TRANSITIONS) {
            if (transition.startsWith(currentStatus + "->")) {
                nextStates.add(transition.split("->")[1]);
            }
        }
        return nextStates;
    }
}