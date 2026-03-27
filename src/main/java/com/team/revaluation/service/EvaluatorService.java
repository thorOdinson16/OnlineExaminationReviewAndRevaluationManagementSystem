package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.team.revaluation.repository.UserRepository;

import java.util.List;

@Service
public class EvaluatorService {

    @Autowired
    private AnswerScriptRepository answerScriptRepository;
    @Autowired
    private UserRepository userRepository;

    /**
     * Returns all scripts that are pending evaluation (status = UNDER_EVALUATION).
     */
    public List<AnswerScript> getPendingScripts() {
        return answerScriptRepository.findByStatus("UNDER_EVALUATION");
    }

    /**
     * Returns all scripts that have been evaluated (status = EVALUATED).
     */
    public List<AnswerScript> getEvaluatedScripts() {
        return answerScriptRepository.findByStatus("EVALUATED");
    }

    /**
     * Submits marks for a script. Validates that the script is in UNDER_EVALUATION,
     * sets the marks, and transitions the status to EVALUATED.
     */
    @Transactional
    public AnswerScript submitMarks(Long scriptId, Float marks) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        if (!"UNDER_EVALUATION".equals(script.getStatus())) {
            throw new InvalidStateTransitionException(
                "Script must be in UNDER_EVALUATION status to submit marks. Current: " + script.getStatus());
        }

        script.setTotalMarks(marks);
        AnswerScriptStateMachine.transition(script, "EVALUATED");
        return answerScriptRepository.save(script);
    }

    /**
     * Verifies a script (publishes results). Validates that the script is in EVALUATED,
     * then transitions to RESULTS_PUBLISHED.
     */
    @Transactional
    public AnswerScript verifyScript(Long scriptId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        if (!"EVALUATED".equals(script.getStatus())) {
            throw new InvalidStateTransitionException(
                "Script must be in EVALUATED status to verify. Current: " + script.getStatus());
        }

        AnswerScriptStateMachine.transition(script, "RESULTS_PUBLISHED");
        return answerScriptRepository.save(script);
    }


    /**
     * Assign an evaluator to a script for evaluation.
     * Contains all business logic for evaluator assignment.
     */
    @Transactional
    public Map<String, Object> assignEvaluatorToScript(Long scriptId, Long evaluatorId) {
        // Validate script exists
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        // Validate evaluator exists
        User user = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new RuntimeException("Evaluator not found with id: " + evaluatorId));

        if (!"EVALUATOR".equals(user.getRole())) {
            throw new RuntimeException("User is not an evaluator");
        }

        // Safe cast to Evaluator
        Evaluator evaluator;
        if (user instanceof Evaluator) {
            evaluator = (Evaluator) user;
        } else {
            evaluator = (Evaluator) userRepository.findById(evaluatorId)
                    .orElseThrow(() -> new RuntimeException("Evaluator not found"));
            if (!(evaluator instanceof Evaluator)) {
                throw new RuntimeException("User is not an evaluator instance");
            }
        }

        // Use state machine to transition (only allowed from SUBMITTED)
        try {
            AnswerScriptStateMachine.transition(script, "UNDER_EVALUATION");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot assign evaluator: " + e.getMessage());
        }

        // Assign evaluator and save
        script.setEvaluator(evaluator);
        AnswerScript updatedScript = answerScriptRepository.save(script);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Evaluator assigned successfully");
        response.put("scriptId", scriptId);
        response.put("evaluatorId", evaluatorId);
        response.put("evaluatorName", evaluator.getName());
        response.put("status", "UNDER_EVALUATION");

        return response;
    }
}