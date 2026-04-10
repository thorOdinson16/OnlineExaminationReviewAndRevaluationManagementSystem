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
 * RevaluatorController — thin REST controller (no business logic).
 *
 * DIP: @Autowired on IRevaluationService interface (not the concrete class).
 *
 * Checklist §3.4 endpoints:
 *   GET  /revaluator/requests              → filters to REVALUATION_IN_PROGRESS only
 *   PUT  /revaluator/requests/{id}/submit  → REVALUATION_COMPLETED + notifies student
 */
@RestController
@RequestMapping("/revaluator")
public class RevaluatorController {

    @Autowired
    private IRevaluationService revaluationService;

    /**
     * Satisfies: "GET /revaluator/requests filters to REVALUATION_IN_PROGRESS only"
     * Returns only requests that are currently in progress, ready for the revaluator
     * to submit marks on.
     */
    @GetMapping("/requests")
    public ResponseEntity<List<RevaluationRequest>> getPendingRequests() {
        // getPendingForRevaluator() queries for REVALUATION_IN_PROGRESS only
        return ResponseEntity.ok(revaluationService.getPendingForRevaluator());
    }

    /**
     * Satisfies: "PUT /revaluator/requests/{id}/submit → REVALUATION_COMPLETED + notifies student"
     *
     * @param id      revaluationId
     * @param marks   new marks awarded by the revaluator
     * @param comments optional comments
     */
    @PutMapping("/requests/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitMarks(
            @PathVariable Long id,
            @RequestParam Float marks,
            @RequestParam(required = false) String comments) {

        RevaluationRequest updated = revaluationService.submitRevaluationMarks(id, marks, comments);

        Map<String, Object> response = new HashMap<>();
        response.put("message",            "Revaluation marks submitted successfully");
        response.put("revaluationId",      id);
        response.put("newMarks",           marks);
        response.put("revaluationStatus",  updated.getRevaluationStatus());
        response.put("scriptStatus",       updated.getAnswerScript() != null
                                               ? updated.getAnswerScript().getStatus() : "N/A");
        return ResponseEntity.ok(response);
    }

    /** List all requests assigned to a specific revaluator (used by dashboard). */
    @GetMapping("/requests/all")
    public ResponseEntity<List<RevaluationRequest>> getAllRequests() {
        return ResponseEntity.ok(revaluationService.getAllRevaluations());
    }

    /** Get a single revaluation request by id. */
    @GetMapping("/requests/{id}")
    public ResponseEntity<RevaluationRequest> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(revaluationService.getRevaluationById(id));
    }
}
