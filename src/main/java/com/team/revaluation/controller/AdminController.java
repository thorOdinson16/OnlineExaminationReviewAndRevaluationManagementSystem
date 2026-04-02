package com.team.revaluation.controller;

import com.team.revaluation.model.*;
import com.team.revaluation.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminController — thin controller, no business logic.
 *
 * All AnswerScript operations now go through ScriptService (not the repo directly).
 * All Review operations go through ReviewService.
 * All Revaluation operations go through RevaluationService.
 * No repository, state machine, or InvalidStateTransitionException is imported here.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private RevaluationService revaluationService;

    @Autowired
    private EvaluatorService evaluatorService;

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private UserService userService;

    // ==================== REVIEW MANAGEMENT ====================

    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewRequest>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/reviews/pending")
    public ResponseEntity<List<ReviewRequest>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    @PutMapping("/reviews/{reviewId}/status")
    public ResponseEntity<ReviewRequest> updateReviewStatus(
            @PathVariable Long reviewId,
            @RequestParam String status) {
        return ResponseEntity.ok(reviewService.updateReviewStatus(reviewId, status));
    }

    @PutMapping("/reviews/{reviewId}/verify")
    public ResponseEntity<ReviewRequest> verifyReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.verifyReview(reviewId));
    }

    // ==================== EVALUATOR ASSIGNMENT ====================

    @PostMapping("/evaluator/assign")
    public ResponseEntity<Map<String, Object>> assignEvaluator(
            @RequestParam Long scriptId,
            @RequestParam Long evaluatorId) {
        return ResponseEntity.ok(evaluatorService.assignEvaluatorToScript(scriptId, evaluatorId));
    }

    // ==================== REVALUATION MANAGEMENT ====================

    @GetMapping("/revaluations")
    public ResponseEntity<List<RevaluationRequest>> getAllRevaluations() {
        return ResponseEntity.ok(revaluationService.getAllRevaluations());
    }

    @GetMapping("/revaluations/pending")
    public ResponseEntity<List<RevaluationRequest>> getPendingRevaluations() {
        return ResponseEntity.ok(revaluationService.getPendingRevaluations());
    }

    @PutMapping("/revaluations/{id}/verify")
    public ResponseEntity<RevaluationRequest> verifyRevaluation(@PathVariable Long id) {
        return ResponseEntity.ok(revaluationService.updateRevaluationStatus(id, "VERIFIED"));
    }

    @PostMapping("/revaluator/assign")
    public ResponseEntity<Map<String, Object>> assignRevaluator(
            @RequestParam Long revaluationId,
            @RequestParam Long revaluatorId) {
        return ResponseEntity.ok(revaluationService.assignRevaluatorToRequest(revaluationId, revaluatorId));
    }

    // ==================== FINAL RESULT MANAGEMENT ====================

    @PutMapping("/results/{scriptId}/publish")
    public ResponseEntity<Map<String, Object>> publishResult(@PathVariable Long scriptId) {
        return ResponseEntity.ok(scriptService.publishResult(scriptId));
    }

    @PutMapping("/results/{scriptId}/finalize")
    public ResponseEntity<Map<String, Object>> finalizeResult(
            @PathVariable Long scriptId,
            @RequestParam(required = false) Float finalMarks) {
        return ResponseEntity.ok(scriptService.finalizeResult(scriptId, finalMarks));
    }

    @PutMapping("/results/bulk-publish")
    public ResponseEntity<Map<String, Object>> bulkPublishResults(@RequestBody List<Long> scriptIds) {
        return ResponseEntity.ok(scriptService.bulkPublishResults(scriptIds));
    }

    @GetMapping("/results/stats")
    public ResponseEntity<Map<String, Object>> getResultStats() {
        return ResponseEntity.ok(scriptService.getResultStats());
    }

    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("pendingReviews",         reviewService.getPendingReviews().size());
        overview.put("pendingRevaluations",     revaluationService.getPendingRevaluations().size());
        overview.put("scriptsUnderEvaluation",  scriptService.getScriptsByStatus("UNDER_EVALUATION").size());
        return ResponseEntity.ok(overview);
    }

    // ==================== USER MANAGEMENT ====================

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/users/stats")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        return ResponseEntity.ok(userService.getUserStats());
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        response.put("userId", userId.toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/revaluators")
    public ResponseEntity<List<Revaluator>> getRevaluators() {
        return ResponseEntity.ok(userService.getRevaluators());
    }

    @GetMapping("/users/evaluators")
    public ResponseEntity<List<Evaluator>> getEvaluators() {
        return ResponseEntity.ok(userService.getEvaluators());
    }
}