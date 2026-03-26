package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;

import java.util.HashSet;
import java.util.Set;

public class AnswerScriptStateMachine {

    private static final Set<String> ALLOWED_TRANSITIONS = new HashSet<>();

    static {
        // Define allowed transitions: "current->next"
        addTransition("SUBMITTED", "UNDER_EVALUATION");
        addTransition("UNDER_EVALUATION", "EVALUATED");
        addTransition("EVALUATED", "RESULTS_PUBLISHED");
        addTransition("EVALUATED", "REVIEW_REQUESTED");
        addTransition("EVALUATED", "REVALUATION_REQUESTED");
        addTransition("RESULTS_PUBLISHED", "FINALIZED");
        addTransition("RESULTS_PUBLISHED", "REVIEW_REQUESTED");
        addTransition("RESULTS_PUBLISHED", "REVALUATION_REQUESTED");
        addTransition("REVIEW_REQUESTED", "REVIEW_IN_PROGRESS");
        addTransition("REVIEW_IN_PROGRESS", "REVIEW_COMPLETED");
        addTransition("REVIEW_COMPLETED", "RESULTS_PUBLISHED");
        addTransition("REVALUATION_REQUESTED", "REVALUATION_IN_PROGRESS");
        addTransition("REVALUATION_IN_PROGRESS", "REVALUATION_COMPLETED");
        addTransition("REVALUATION_COMPLETED", "FINALIZED");
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
}