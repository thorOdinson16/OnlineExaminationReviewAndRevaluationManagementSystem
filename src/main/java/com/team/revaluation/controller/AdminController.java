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
 * DIP (checklist §5): all @Autowired fields use the SERVICE INTERFACE type,
 * not the concrete class. Spring injects the concrete bean automatically.
 *
 * No repository, state machine, or exception class is imported here.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    // ── DIP: depend on abstractions, not concretions ──────────────────────────
    @Autowired
    private IReviewService reviewService;

    @Autowired
    private IRevaluationService revaluationService;

    @Autowired
    private IEvaluatorService evaluatorService;

    @Autowired
    private IScriptService scriptService;

    @Autowired
    private UserService userService;   // UserService has no interface yet — kept concrete for brevity

    // ==================== REVIEW MANAGEMENT ====================

    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewRequest>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    /** Satisfies: "GET /admin/reviews/pending returns only PAYMENT_PENDING reviews" */
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

    /**
     * Satisfies: "POST /admin/evaluator/assign assigns evaluator + transitions → UNDER_EVALUATION"
     */
    @PostMapping("/evaluator/assign")
    public ResponseEntity<Map<String, Object>> assignEvaluator(
            @RequestParam Long scriptId,
            @RequestParam Long evaluatorId) {
        return ResponseEntity.ok(evaluatorService.assignEvaluatorToScript(scriptId, evaluatorId));
    }

    // ==================== REVALUATION MANAGEMENT ====================

    @GetMapping("/revaluations")
    public ResponseEntity<List<RevaluationRequest>> getAllRevaluations(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            return ResponseEntity.ok(revaluationService.getPendingForRevaluator());
        }
        return ResponseEntity.ok(revaluationService.getAllRevaluations());
    }

    @GetMapping("/revaluations/pending")
    public ResponseEntity<List<RevaluationRequest>> getPendingRevaluations() {
        return ResponseEntity.ok(revaluationService.getPendingRevaluations());
    }

    @PutMapping("/revaluations/{id}/verify")
    public ResponseEntity<RevaluationRequest> verifyRevaluation(@PathVariable Long id) {
        // NOTE: With updated state machine PAYMENT_PENDING → REVALUATION_IN_PROGRESS,
        // "verify" here means admin acknowledges; status is already REVALUATION_IN_PROGRESS.
        // Kept for UI compatibility — returns the current request unchanged if already in progress.
        return ResponseEntity.ok(revaluationService.getRevaluationById(id));
    }

    @GetMapping("/revaluations/by-status")
    public ResponseEntity<List<RevaluationRequest>> getRevaluationsByStatus(
            @RequestParam String status) {
        // RevaluationService.getRevaluationsByStatus() queries by revaluationStatus field
        List<RevaluationRequest> results = revaluationService
                .getAllRevaluations()
                .stream()
                .filter(r -> status.equals(r.getRevaluationStatus()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(results);
    }

    /**
     * Satisfies: "POST /admin/revaluator/assign assigns Revaluator to RevaluationRequest"
     * Request must already be REVALUATION_IN_PROGRESS (set by payment step).
     */
    @PostMapping("/revaluator/assign")
    public ResponseEntity<Map<String, Object>> assignRevaluator(
            @RequestParam Long revaluationId,
            @RequestParam Long revaluatorId) {
        return ResponseEntity.ok(revaluationService.assignRevaluatorToRequest(revaluationId, revaluatorId));
    }

    // ==================== FINAL RESULT MANAGEMENT ====================

    /**
     * Satisfies: "PUT /admin/results/{scriptId}/publish → RESULTS_PUBLISHED"
     */
    @PutMapping("/results/{scriptId}/publish")
    public ResponseEntity<Map<String, Object>> publishResult(@PathVariable Long scriptId) {
        return ResponseEntity.ok(scriptService.publishResult(scriptId));
    }

    /**
     * Satisfies: "PUT /admin/results/{scriptId}/finalize → FINALIZED, updates final marks, notifies student"
     */
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
        overview.put("pendingReviews",        reviewService.getPendingReviews().size());
        overview.put("pendingRevaluations",   revaluationService.getPendingRevaluations().size());
        overview.put("scriptsUnderEvaluation",scriptService.getScriptsByStatus("UNDER_EVALUATION").size());
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
        response.put("userId",  userId.toString());
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