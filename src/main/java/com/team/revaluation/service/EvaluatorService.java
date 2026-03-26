package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EvaluatorService {

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

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
}