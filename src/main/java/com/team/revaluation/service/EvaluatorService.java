package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.Evaluator;
import com.team.revaluation.model.User;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EvaluatorService — owns all evaluator business logic.
 *
 * All AnswerScript status changes go through AnswerScriptStateMachine (State pattern).
 * No raw script.setStatus() calls exist here.
 *
 * Implements IEvaluatorService for DIP compliance.
 */
@Service
public class EvaluatorService implements IEvaluatorService {

    @Autowired private AnswerScriptRepository answerScriptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    // ── Checklist §3.2: GET /evaluator/scripts/pending ───────────────────────
    @Override
    public List<AnswerScript> getPendingScripts() {
        return answerScriptRepository.findByStatus("UNDER_EVALUATION");
    }

    @Override
    public List<AnswerScript> getEvaluatedScripts() {
        return answerScriptRepository.findByStatus("EVALUATED");
    }

    // ── Checklist §3.2: PUT /evaluator/scripts/{scriptId}/submit ─────────────
    // Goes through StateMachine → EVALUATED
    @Transactional
    @Override
    public AnswerScript submitMarks(Long scriptId, Float marks) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
            .orElseThrow(() -> new RuntimeException("Script not found: " + scriptId));

        if (marks == null || marks < 0) {
            throw new RuntimeException("Invalid marks: " + marks);
        }

        script.setTotalMarks(marks);

        try {
            AnswerScriptStateMachine.transition(script, "EVALUATED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("State transition failed: " + e.getMessage());
        }

        AnswerScript saved = answerScriptRepository.save(script);

        if (saved.getStudent() != null) {
            notificationService.notifyStudent(saved.getStudent(),
                "Marks submitted for Script #" + scriptId + ". Marks: " + marks);
        }
        return saved;
    }

    // ── Checklist §3.2: PUT /evaluator/scripts/{scriptId}/verify ─────────────
    // Transitions EVALUATED → RESULTS_PUBLISHED
    @Transactional
    @Override
    public AnswerScript verifyScript(Long scriptId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
            .orElseThrow(() -> new RuntimeException("Script not found: " + scriptId));

        try {
            AnswerScriptStateMachine.transition(script, "RESULTS_PUBLISHED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("State transition failed: " + e.getMessage());
        }

        AnswerScript saved = answerScriptRepository.save(script);

        if (saved.getStudent() != null) {
            notificationService.notifyStudent(saved.getStudent(),
                "Results published for Script #" + scriptId + ". Please check your results.");
        }
        return saved;
    }

    // ── Checklist §3.2: POST /admin/evaluator/assign ─────────────────────────
    // Assigns evaluator + transitions SUBMITTED → UNDER_EVALUATION
    @Transactional
    @Override
    public Map<String, Object> assignEvaluatorToScript(Long scriptId, Long evaluatorId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
            .orElseThrow(() -> new RuntimeException("Script not found: " + scriptId));

        User user = userRepository.findById(evaluatorId)
            .orElseThrow(() -> new RuntimeException("User not found: " + evaluatorId));

        if (!"EVALUATOR".equals(user.getRole())) {
            throw new RuntimeException("User is not an evaluator");
        }

        Evaluator evaluator = (Evaluator) user;
        script.setEvaluator(evaluator);

        try {
            AnswerScriptStateMachine.transition(script, "UNDER_EVALUATION");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot assign evaluator: " + e.getMessage());
        }

        AnswerScript saved = answerScriptRepository.save(script);

        if (saved.getStudent() != null) {
            notificationService.notifyStudent(saved.getStudent(),
                "Your answer script #" + scriptId + " has been assigned to an evaluator.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message",       "Evaluator assigned successfully");
        response.put("scriptId",      scriptId);
        response.put("evaluatorId",   evaluatorId);
        response.put("evaluatorName", evaluator.getName());
        response.put("status",        saved.getStatus());
        return response;
    }
}