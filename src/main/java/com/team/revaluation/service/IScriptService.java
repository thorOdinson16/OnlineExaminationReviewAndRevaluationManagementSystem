// ============================================================
// FILE: src/main/java/com/team/revaluation/service/IScriptService.java
// ============================================================
package com.team.revaluation.service;

import com.team.revaluation.model.AnswerScript;
import java.util.List;
import java.util.Map;

/**
 * DIP interface for ScriptService (checklist §5 — Dependency Inversion Principle).
 */
public interface IScriptService {
    List<AnswerScript> getAllScripts();
    AnswerScript getScriptById(Long scriptId);
    List<AnswerScript> getScriptsByStudent(Long studentId);
    List<AnswerScript> getScriptsByStatus(String status);
    Map<String, Object> publishResult(Long scriptId);
    Map<String, Object> finalizeResult(Long scriptId, Float finalMarks);
    Map<String, Object> bulkPublishResults(List<Long> scriptIds);
    Map<String, Object> getResultStats();
}