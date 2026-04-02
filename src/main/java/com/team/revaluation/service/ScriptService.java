package com.team.revaluation.service;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ScriptService owns all AnswerScript business logic.
 *
 * Previously AdminController was calling answerScriptRepository and
 * AnswerScriptStateMachine directly — violating the MVC rule that controllers
 * must not contain business logic or direct repository calls.
 * All of that logic now lives here.
 */
@Service
public class ScriptService {

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private NotificationService notificationService;

    // ==================== READ ====================

    public List<AnswerScript> getAllScripts() {
        return answerScriptRepository.findAllWithDetails();
    }

    public AnswerScript getScriptById(Long scriptId) {
        return answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));
    }

    public List<AnswerScript> getScriptsByStudent(Long studentId) {
        return answerScriptRepository.findByStudentUserId(studentId);
    }

    public List<AnswerScript> getScriptsByStatus(String status) {
        return answerScriptRepository.findByStatus(status);
    }

    // ==================== PUBLISH / FINALIZE ====================

    @Transactional
    public Map<String, Object> publishResult(Long scriptId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        try {
            AnswerScriptStateMachine.transition(script, "RESULTS_PUBLISHED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot publish results: " + e.getMessage());
        }

        AnswerScript saved = answerScriptRepository.save(script);

        if (saved.getStudent() != null) {
            notificationService.notifyStudent(saved.getStudent(),
                String.format("Results published for Script #%d. Marks: %.2f",
                    scriptId, saved.getTotalMarks()));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Results published successfully");
        response.put("scriptId", scriptId);
        response.put("studentId", saved.getStudent() != null ? saved.getStudent().getUserId() : null);
        response.put("status", saved.getStatus());
        response.put("totalMarks", saved.getTotalMarks());
        return response;
    }

    @Transactional
    public Map<String, Object> finalizeResult(Long scriptId, Float finalMarks) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + scriptId));

        if (finalMarks != null) {
            script.setTotalMarks(finalMarks);
        }

        try {
            AnswerScriptStateMachine.transition(script, "FINALIZED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot finalize results: " + e.getMessage());
        }

        AnswerScript saved = answerScriptRepository.save(script);

        if (saved.getStudent() != null) {
            notificationService.notifyStudent(saved.getStudent(),
                String.format("Results finalized for Script #%d. Final marks: %.2f (no further changes allowed)",
                    scriptId, saved.getTotalMarks()));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Result finalized successfully");
        response.put("scriptId", scriptId);
        response.put("studentId", saved.getStudent() != null ? saved.getStudent().getUserId() : null);
        response.put("status", saved.getStatus());
        response.put("totalMarks", saved.getTotalMarks());
        response.put("finalizedAt", java.time.LocalDateTime.now().toString());
        return response;
    }

    @Transactional
    public Map<String, Object> bulkPublishResults(List<Long> scriptIds) {
        int published = 0;
        int failed = 0;

        for (Long scriptId : scriptIds) {
            try {
                AnswerScript script = answerScriptRepository.findById(scriptId).orElse(null);
                if (script == null) { failed++; continue; }

                AnswerScriptStateMachine.transition(script, "RESULTS_PUBLISHED");
                answerScriptRepository.save(script);
                published++;

                if (script.getStudent() != null) {
                    notificationService.notifyStudent(script.getStudent(),
                        "Results published for Script #" + scriptId);
                }
            } catch (Exception e) {
                failed++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bulk publish completed");
        response.put("published", published);
        response.put("failed", failed);
        return response;
    }

    // ==================== STATS ====================

    public Map<String, Object> getResultStats() {
        List<AnswerScript> all = answerScriptRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalScripts", all.size());
        stats.put("published",       count(all, "RESULTS_PUBLISHED"));
        stats.put("finalized",        count(all, "FINALIZED"));
        stats.put("evaluated",        count(all, "EVALUATED"));
        stats.put("underEvaluation",  count(all, "UNDER_EVALUATION"));
        return stats;
    }

    private long count(List<AnswerScript> scripts, String status) {
        return scripts.stream().filter(s -> status.equals(s.getStatus())).count();
    }
}