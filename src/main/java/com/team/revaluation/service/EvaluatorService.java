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
 * Implements IEvaluatorService for DIP (checklist §5).
 */
@Service
public class EvaluatorService implements IEvaluatorService {

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns all scripts pending evaluation (UNDER_EVALUATION).
     * Satisfies: "GET /evaluator/scripts/pending filters by UNDER_EVALUATION status"
     */
    @Override
    public List<AnswerScript> getPendingScripts() {
        return answerScriptRepository.findByStatus("UNDER_EVALUATION");
    }

    @Override
    public List<AnswerScript> getEvaluatedScripts() {
        return answerScriptRepository.findByStatus("EVALUATED");
    }

    /**
     * Submits marks; transitions UNDER_EVALUATION → EVALUATED via StateMachine.
     * Satisfies: "PUT /evaluator/scripts/{scriptId}/submit goes through StateMachine → EVALUATED"
     */
    @Override
    @Transactional
    public AnswerScript submitMarks(Long scriptId, Float marks) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        if (!"UNDER_EVALUATION".equals(script.getStatus())) {
            throw new InvalidStateTransitionException(
                "Script must be UNDER_EVALUATION to submit marks. Current: " + script.getStatus());
        }

        script.setTotalMarks(marks);
        AnswerScriptStateMachine.transition(script, "EVALUATED");   // State Machine enforces transition
        return answerScriptRepository.save(script);
    }

    /**
     * Publishes results; transitions EVALUATED → RESULTS_PUBLISHED via StateMachine.
     * Satisfies: "PUT /evaluator/scripts/{scriptId}/verify transitions → RESULTS_PUBLISHED"
     */
    @Override
    @Transactional
    public AnswerScript verifyScript(Long scriptId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        if (!"EVALUATED".equals(script.getStatus())) {
            throw new InvalidStateTransitionException(
                "Script must be EVALUATED to publish. Current: " + script.getStatus());
        }

        AnswerScriptStateMachine.transition(script, "RESULTS_PUBLISHED");
        return answerScriptRepository.save(script);
    }

    /**
     * Assigns an evaluator to a script; transitions SUBMITTED → UNDER_EVALUATION via StateMachine.
     * Satisfies: "POST /admin/evaluator/assign assigns evaluator + transitions → UNDER_EVALUATION"
     */
    @Override
    @Transactional
    public Map<String, Object> assignEvaluatorToScript(Long scriptId, Long evaluatorId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        User user = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new RuntimeException("Evaluator not found with id: " + evaluatorId));

        if (!"EVALUATOR".equals(user.getRole())) {
            throw new RuntimeException("User is not an evaluator");
        }

        Evaluator evaluator = (Evaluator) user;

        try {
            AnswerScriptStateMachine.transition(script, "UNDER_EVALUATION");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot assign evaluator: " + e.getMessage());
        }

        script.setEvaluator(evaluator);
        answerScriptRepository.save(script);

        Map<String, Object> response = new HashMap<>();
        response.put("message",       "Evaluator assigned successfully");
        response.put("scriptId",      scriptId);
        response.put("evaluatorId",   evaluatorId);
        response.put("evaluatorName", evaluator.getName());
        response.put("status",        "UNDER_EVALUATION");
        return response;
    }
}