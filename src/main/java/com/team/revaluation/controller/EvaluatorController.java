package com.team.revaluation.controller;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.service.EvaluatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/evaluator")
public class EvaluatorController {

    @Autowired
    private EvaluatorService evaluatorService;

    // Pending scripts (UNDER_EVALUATION) — matches checklist: GET /evaluator/scripts/pending
    @GetMapping("/scripts/pending")
    public ResponseEntity<List<AnswerScript>> getPendingScriptsByStatus() {
        return ResponseEntity.ok(evaluatorService.getPendingScripts());
    }

    // Alias without /pending — keeps existing UI calls working
    @GetMapping("/scripts")
    public ResponseEntity<List<AnswerScript>> getPendingScripts() {
        return ResponseEntity.ok(evaluatorService.getPendingScripts());
    }

    // Scripts that are already evaluated (EVALUATED)
    @GetMapping("/scripts/evaluated")
    public ResponseEntity<List<AnswerScript>> getEvaluatedScripts() {
        return ResponseEntity.ok(evaluatorService.getEvaluatedScripts());
    }

    // Submit marks for a script — goes through StateMachine via service
    @PutMapping("/scripts/{scriptId}/submit")
    public ResponseEntity<Map<String, Object>> submitMarks(
            @PathVariable Long scriptId,
            @RequestParam Float marks) {
        AnswerScript updated = evaluatorService.submitMarks(scriptId, marks);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Marks submitted successfully");
        response.put("scriptId", scriptId);
        response.put("totalMarks", updated.getTotalMarks());
        response.put("status", updated.getStatus());
        return ResponseEntity.ok(response);
    }

    // Verify (publish) an evaluated script — transitions to RESULTS_PUBLISHED
    @PutMapping("/scripts/{scriptId}/verify")
    public ResponseEntity<Map<String, Object>> verifyScript(@PathVariable Long scriptId) {
        AnswerScript updated = evaluatorService.verifyScript(scriptId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Script verified and results published");
        response.put("scriptId", scriptId);
        response.put("status", updated.getStatus());
        return ResponseEntity.ok(response);
    }
}