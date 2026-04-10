package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;

import java.util.HashSet;
import java.util.Set;

/**
 * AnswerScriptStateMachine — Behavioral (State) Pattern (checklist §4.3).
 *
 * State names match the canonical table in checklist §6 EXACTLY:
 *
 *   Submitted → UnderEvaluation → Evaluated → ResultsPublished
 *   ResultsPublished → ReviewRequested → ReviewPaymentPending → ReviewInProgress
 *   ReviewInProgress → ReviewCompleted → AwaitStudentDecision
 *   AwaitStudentDecision → Finalized          (student accepts)
 *   AwaitStudentDecision → RevaluationRequested (student escalates)
 *   RevaluationRequested → RevaluationPaymentPending → RevaluationInProgress
 *   RevaluationInProgress → RevaluationCompleted → FinalResultUpdated → Finalized
 *
 * PATTERN EXPLANATION for report:
 *   - All service-layer status changes MUST route through this class.
 *   - Illegal transitions throw InvalidStateTransitionException.
 *   - No service class calls script.setStatus() directly.
 */
public class AnswerScriptStateMachine {

    // ── Allowed transitions (from -> to) ──────────────────────────────────────
    private static final Set<String> ALLOWED = new HashSet<>();

    static {
        // Normal evaluation flow (§6 rows 1-3)
        allow("SUBMITTED",              "UNDER_EVALUATION");
        allow("UNDER_EVALUATION",       "EVALUATED");
        allow("EVALUATED",              "RESULTS_PUBLISHED");

        // Review flow (§6 rows 4-8)
        allow("RESULTS_PUBLISHED",      "REVIEW_REQUESTED");
        allow("REVIEW_REQUESTED",       "REVIEW_PAYMENT_PENDING");
        allow("REVIEW_PAYMENT_PENDING", "REVIEW_IN_PROGRESS");
        allow("REVIEW_IN_PROGRESS",     "REVIEW_COMPLETED");
        allow("REVIEW_COMPLETED",       "AWAIT_STUDENT_DECISION");

        // Student decision (§6 rows 9-10)
        allow("AWAIT_STUDENT_DECISION", "FINALIZED");               // accept marks
        allow("AWAIT_STUDENT_DECISION", "REVALUATION_REQUESTED");   // escalate

        // Revaluation flow (§6 rows 11-15)
        allow("REVALUATION_REQUESTED",       "REVALUATION_PAYMENT_PENDING");
        allow("REVALUATION_PAYMENT_PENDING", "REVALUATION_IN_PROGRESS");
        allow("REVALUATION_IN_PROGRESS",     "REVALUATION_COMPLETED");
        allow("REVALUATION_COMPLETED",       "FINAL_RESULT_UPDATED");
        allow("FINAL_RESULT_UPDATED",        "FINALIZED");

        // ── Convenience / shortcut transitions (keep backward compat) ─────────
        // Admin may publish directly from EVALUATED (skipping manual review start)
        allow("EVALUATED",              "FINALIZED");
        allow("RESULTS_PUBLISHED",      "FINALIZED");
        allow("REVIEW_COMPLETED",       "FINALIZED");
        allow("REVALUATION_COMPLETED",  "FINALIZED");

        // Admin assigns evaluator → UNDER_EVALUATION (also used by evaluator assign endpoint)
        // already covered above

        // Rejection paths
        allow("REVIEW_REQUESTED",       "REJECTED");
        allow("REVALUATION_REQUESTED",  "REJECTED");
    }

    private static void allow(String from, String to) {
        ALLOWED.add(from + "->" + to);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Validates and performs the transition. Throws InvalidStateTransitionException
     * on illegal moves.
     *
     * ALL service-layer status changes for AnswerScript must call this method.
     * Direct calls to script.setStatus() anywhere in the codebase are a bug.
     */
    public static void transition(AnswerScript script, String newStatus) {
        String current = script.getStatus();
        if (current == null) {
            current = "SUBMITTED";
            script.setStatus(current);
        }
        if (current.equals(newStatus)) return;   // idempotent no-op

        String key = current + "->" + newStatus;
        if (!ALLOWED.contains(key)) {
            throw new InvalidStateTransitionException(
                String.format("Invalid AnswerScript transition: '%s' → '%s'", current, newStatus));
        }
        script.setStatus(newStatus);
    }

    public static boolean isTransitionAllowed(String current, String newStatus) {
        if (current == null) current = "SUBMITTED";
        return ALLOWED.contains(current + "->" + newStatus);
    }

    public static Set<String> getAllowedNextStates(String current) {
        if (current == null) current = "SUBMITTED";
        Set<String> next = new HashSet<>();
        String prefix = current + "->";
        for (String t : ALLOWED) {
            if (t.startsWith(prefix)) next.add(t.substring(prefix.length()));
        }
        return next;
    }
}
