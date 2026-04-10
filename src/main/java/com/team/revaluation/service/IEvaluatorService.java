// ============================================================
// FILE: src/main/java/com/team/revaluation/service/IEvaluatorService.java
// ============================================================
package com.team.revaluation.service;

import com.team.revaluation.model.AnswerScript;
import java.util.List;
import java.util.Map;

/**
 * DIP interface for EvaluatorService (checklist §5 — Dependency Inversion Principle).
 */
public interface IEvaluatorService {
    List<AnswerScript> getPendingScripts();
    List<AnswerScript> getEvaluatedScripts();
    AnswerScript submitMarks(Long scriptId, Float marks);
    AnswerScript verifyScript(Long scriptId);
    Map<String, Object> assignEvaluatorToScript(Long scriptId, Long evaluatorId);
}