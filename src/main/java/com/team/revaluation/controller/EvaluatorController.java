package com.team.revaluation.controller;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.service.IEvaluatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EvaluatorController — thin controller, no business logic.
 *
 * DIP (checklist §5): @Autowired on IEvaluatorService, not on EvaluatorService directly.
 *
 * Checklist §3.2 endpoint mapping:
 *   GET  /evaluator/scripts/pending              → filters by UNDER_EVALUATION status
 *   PUT  /evaluator/scripts/{scriptId}/submit    → goes through StateMachine → EVALUATED
 *   PUT  /evaluator/scripts/{scriptId}/verify    → transitions → RESULTS_PUBLISHED
 */
@RestController
@RequestMapping("/evaluator")
public class EvaluatorController {

    // DIP: depend on the abstraction
    @Autowired
    private IEvaluatorService evaluatorService;

    /**
     * Satisfies: "GET /evaluator/scripts/pending filters by UNDER_EVALUATION status"
     */
    @GetMapping("/scripts/pending")
    public ResponseEntity<List<AnswerScript>> getPendingScripts() {
        return ResponseEntity.ok(evaluatorService.getPendingScripts());
    }

    @GetMapping("/scripts/evaluated")
    public ResponseEntity<List<AnswerScript>> getEvaluatedScripts() {
        return ResponseEntity.ok(evaluatorService.getEvaluatedScripts());
    }

    /**
     * Satisfies: "PUT /evaluator/scripts/{scriptId}/submit → StateMachine → EVALUATED"
     */
    @PutMapping("/scripts/{scriptId}/submit")
    public ResponseEntity<Map<String, Object>> submitMarks(
            @PathVariable Long scriptId,
            @RequestParam Float marks) {
        AnswerScript updated = evaluatorService.submitMarks(scriptId, marks);
        Map<String, Object> response = new HashMap<>();
        response.put("message",    "Marks submitted successfully");
        response.put("scriptId",   scriptId);
        response.put("marks",      marks);
        response.put("status",     updated.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * Satisfies: "PUT /evaluator/scripts/{scriptId}/verify → RESULTS_PUBLISHED"
     */
    @PutMapping("/scripts/{scriptId}/verify")
    public ResponseEntity<Map<String, Object>> verifyScript(@PathVariable Long scriptId) {
        AnswerScript updated = evaluatorService.verifyScript(scriptId);
        Map<String, Object> response = new HashMap<>();
        response.put("message",  "Results published successfully");
        response.put("scriptId", scriptId);
        response.put("status",   updated.getStatus());
        return ResponseEntity.ok(response);
    }
}