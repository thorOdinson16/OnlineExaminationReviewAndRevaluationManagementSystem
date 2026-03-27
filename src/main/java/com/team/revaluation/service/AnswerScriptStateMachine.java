package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;

import java.util.HashSet;
import java.util.Set;

/**
 * State Machine for AnswerScript following the official flowchart from the checklist.
 * 
 * Valid States:
 * - SUBMITTED (initial)
 * - UNDER_EVALUATION
 * - EVALUATED
 * - RESULTS_PUBLISHED
 * - REVIEW_REQUESTED
 * - REVIEW_IN_PROGRESS
 * - REVIEW_COMPLETED
 * - REVALUATION_REQUESTED
 * - REVALUATION_IN_PROGRESS
 * - REVALUATION_COMPLETED
 * - FINALIZED (terminal)
 * - AWAIT_STUDENT_DECISION
 */
public class AnswerScriptStateMachine {

    private static final Set<String> ALLOWED_TRANSITIONS = new HashSet<>();

    static {
        // ==================== NORMAL EVALUATION FLOW ====================
        addTransition("SUBMITTED", "UNDER_EVALUATION");      // Admin assigns evaluator
        addTransition("UNDER_EVALUATION", "EVALUATED");       // Evaluator submits marks
        addTransition("EVALUATED", "RESULTS_PUBLISHED");      // Evaluator/Admin publishes results

        // ==================== REVIEW FLOW ====================
        // Student requests review after results are published
        addTransition("RESULTS_PUBLISHED", "REVIEW_REQUESTED");  // Student applies for review
        addTransition("REVIEW_REQUESTED", "REVIEW_IN_PROGRESS"); // Admin assigns reviewer
        addTransition("REVIEW_IN_PROGRESS", "REVIEW_COMPLETED"); // Reviewer completes
        addTransition("REVIEW_COMPLETED", "AWAIT_STUDENT_DECISION"); // System waits for student
        addTransition("AWAIT_STUDENT_DECISION", "FINALIZED");      // Student accepts marks
        addTransition("AWAIT_STUDENT_DECISION", "REVALUATION_REQUESTED"); // Student requests revaluation
        addTransition("REVIEW_COMPLETED", "RESULTS_PUBLISHED");    // Alternative: auto-publish after review

        // ==================== REVALUATION FLOW ====================
        // Student requests revaluation from published results or after review
        addTransition("RESULTS_PUBLISHED", "REVALUATION_REQUESTED"); // Student applies for revaluation
        addTransition("REVALUATION_REQUESTED", "REVALUATION_IN_PROGRESS"); // Admin assigns revaluator
        addTransition("REVALUATION_IN_PROGRESS", "REVALUATION_COMPLETED"); // Revaluator submits marks
        addTransition("REVALUATION_COMPLETED", "FINALIZED");            // Admin finalizes

        // ==================== FINALIZATION ====================
        addTransition("RESULTS_PUBLISHED", "FINALIZED");           // Admin finalizes without changes
        addTransition("REVIEW_COMPLETED", "FINALIZED");            // Admin finalizes after review
        addTransition("REVALUATION_COMPLETED", "FINALIZED");       // Admin finalizes after revaluation
        
        // ==================== REJECTION FLOW ====================
        addTransition("REVIEW_REQUESTED", "REJECTED");             // Admin rejects review
        addTransition("REVALUATION_REQUESTED", "REJECTED");        // Admin rejects revaluation
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