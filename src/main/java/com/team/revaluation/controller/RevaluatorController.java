package com.team.revaluation.controller;

import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.service.IRevaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RevaluatorController — thin controller, no business logic.
 *
 * DIP (checklist §5): @Autowired on IRevaluationService, not on RevaluationService directly.
 *
 * Checklist §3.4 endpoint mapping:
 *   GET /revaluator/requests              → filters to REVALUATION_IN_PROGRESS only
 *   PUT /revaluator/requests/{id}/submit  → REVALUATION_COMPLETED + notifies student
 */
@RestController
@RequestMapping("/revaluator")
public class RevaluatorController {

    // DIP: depend on the abstraction
    @Autowired
    private IRevaluationService revaluationService;

    /**
     * Satisfies: "GET /revaluator/requests filters to REVALUATION_IN_PROGRESS only"
     */
    @GetMapping("/requests")
    public ResponseEntity<List<RevaluationRequest>> getAllRequests() {
        return ResponseEntity.ok(revaluationService.getPendingForRevaluator());
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<RevaluationRequest>> getPendingRequests() {
        return ResponseEntity.ok(revaluationService.getPendingForRevaluator());
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<RevaluationRequest> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(revaluationService.getRevaluationById(id));
    }

    /**
     * Satisfies: "PUT /revaluator/requests/{id}/submit → REVALUATION_COMPLETED + notifies student"
     */
    @PutMapping("/requests/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitRevaluationMarks(
            @PathVariable Long id,
            @RequestParam Float marks,
            @RequestParam(required = false) String comments) {

        RevaluationRequest savedRequest = revaluationService.submitRevaluationMarks(id, marks, comments);

        Map<String, Object> response = new HashMap<>();
        response.put("message",   "Revaluation marks submitted successfully");
        response.put("requestId", id);
        response.put("scriptId",  savedRequest.getAnswerScript().getScriptId());
        response.put("newMarks",  marks);
        response.put("status",    savedRequest.getRevaluationStatus());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        long pending   = revaluationService.countByStatus("REVALUATION_IN_PROGRESS");
        long completed = revaluationService.countByStatus("REVALUATION_COMPLETED");

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount",   pending);
        stats.put("completedCount", completed);
        stats.put("totalCount",     pending + completed);

        return ResponseEntity.ok(stats);
    }
}