// File: src/main/java/com/team/revaluation/controller/AdminController.java
package com.team.revaluation.controller;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.Evaluator;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.User;
import com.team.revaluation.model.Revaluator;
import com.team.revaluation.service.ReviewService;

import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.service.AnswerScriptStateMachine;

import com.team.revaluation.service.RevaluationService;
import com.team.revaluation.service.NotificationService;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.UserRepository;
import com.team.revaluation.repository.RevaluationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private RevaluationService revaluationService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private AnswerScriptRepository answerScriptRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RevaluationRequestRepository revaluationRequestRepository;

    // ==================== REVIEW MANAGEMENT ====================
    
    // View all review requests
    @GetMapping("/reviews")
    public ResponseEntity<List<ReviewRequest>> getAllReviews() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }
    
    // Get pending reviews for verification (PAYMENT_PENDING status)
    @GetMapping("/reviews/pending")
    public ResponseEntity<List<ReviewRequest>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    // Verify / update review request status
    @PutMapping("/reviews/{reviewId}/status")
    public ResponseEntity<ReviewRequest> updateReviewStatus(
            @PathVariable Long reviewId,
            @RequestParam String status) {
        return ResponseEntity.ok(reviewService.updateReviewStatus(reviewId, status));
    }
    
    // Verify a review request (admin approval)
    @PutMapping("/reviews/{reviewId}/verify")
    public ResponseEntity<ReviewRequest> verifyReview(@PathVariable Long reviewId) {
        // This transitions from PAYMENT_PENDING → VERIFIED → IN_PROGRESS
        ReviewRequest verified = reviewService.verifyReview(reviewId);
        return ResponseEntity.ok(verified);
    }

    // ==================== EVALUATOR ASSIGNMENT ====================
    
    // Assign evaluator to script
    // Replace the existing assignEvaluator method with this one

    @PostMapping("/evaluator/assign")
    public ResponseEntity<Map<String, Object>> assignEvaluator(
            @RequestParam Long scriptId,
            @RequestParam Long evaluatorId) {

        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));

        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new RuntimeException("Evaluator not found"));

        if (!"EVALUATOR".equals(evaluator.getRole())) {
            throw new RuntimeException("User is not an evaluator");
        }

        // Use state machine to transition (only allowed from SUBMITTED)
        try {
            AnswerScriptStateMachine.transition(script, "UNDER_EVALUATION");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot assign evaluator: " + e.getMessage());
        }

        script.setEvaluator((Evaluator) evaluator);
        AnswerScript updatedScript = answerScriptRepository.save(script);

        // Notify evaluator (optional)
        notificationService.notifyStudent(null, "Script #" + scriptId + " assigned for evaluation to " + evaluator.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Evaluator assigned successfully");
        response.put("scriptId", scriptId);
        response.put("evaluatorId", evaluatorId);
        response.put("evaluatorName", evaluator.getName());
        response.put("status", "UNDER_EVALUATION");

        return ResponseEntity.ok(response);
    }
    
    // ==================== REVALUATION MANAGEMENT ====================
    
    // Get all revaluation requests
    @GetMapping("/revaluations")
    public ResponseEntity<List<RevaluationRequest>> getAllRevaluations() {
        return ResponseEntity.ok(revaluationService.getAllRevaluations());
    }
    
    // Get pending revaluation requests
    @GetMapping("/revaluations/pending")
    public ResponseEntity<List<RevaluationRequest>> getPendingRevaluations() {
        return ResponseEntity.ok(revaluationService.getPendingRevaluations());
    }
    
    // Verify revaluation request
    @PutMapping("/revaluations/{id}/verify")
    public ResponseEntity<RevaluationRequest> verifyRevaluation(
            @PathVariable Long id) {
        return ResponseEntity.ok(revaluationService.updateRevaluationStatus(id, "VERIFIED"));
    }
    
    // Assign revaluator to revaluation request
    @PostMapping("/revaluator/assign")
    public ResponseEntity<Map<String, Object>> assignRevaluator(
            @RequestParam Long revaluationId,
            @RequestParam Long revaluatorId) {
        
        RevaluationRequest request = revaluationRequestRepository.findById(revaluationId)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
        
        User revaluator = userRepository.findById(revaluatorId)
                .orElseThrow(() -> new RuntimeException("Revaluator not found"));
        
        if (!"REVALUATOR".equals(revaluator.getRole())) {
            throw new RuntimeException("User is not a revaluator");
        }
        
        request.setRevaluator((Revaluator) revaluator);
        request.setRevaluationStatus("REVALUATION_IN_PROGRESS");
        RevaluationRequest updatedRequest = revaluationRequestRepository.save(request);
        
        // Notify student about assignment
        notificationService.notifyStudent(request.getStudent(),
            "Revaluation request #" + revaluationId + " assigned to revaluator: " + revaluator.getName());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Revaluator assigned successfully");
        response.put("revaluationId", revaluationId);
        response.put("revaluatorId", revaluatorId);
        response.put("revaluatorName", revaluator.getName());
        response.put("status", "REVALUATION_IN_PROGRESS");
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== FINAL RESULT MANAGEMENT ====================
    
    // Publish results - sets script status to RESULTS_PUBLISHED
    @PutMapping("/results/{scriptId}/publish")
    public ResponseEntity<Map<String, Object>> publishResult(@PathVariable Long scriptId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        
        // Validate current status
        if (!"EVALUATED".equals(script.getStatus()) && !"REVIEW_COMPLETED".equals(script.getStatus())) {
            throw new RuntimeException("Cannot publish results. Script is in status: " + script.getStatus());
        }
        
        try {
            AnswerScriptStateMachine.transition(script, "RESULTS_PUBLISHED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot publish results: " + e.getMessage());
        }
        AnswerScript updatedScript = answerScriptRepository.save(script);
        
        // Notify student about result publication
        if (script.getStudent() != null) {
            notificationService.notifyStudent(script.getStudent(),
                String.format("📢 Results published for Script #%d. Final marks: %.2f", 
                    scriptId, script.getTotalMarks()));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Results published successfully");
        response.put("scriptId", scriptId);
        response.put("studentId", script.getStudent() != null ? script.getStudent().getUserId() : null);
        response.put("status", "RESULTS_PUBLISHED");
        response.put("totalMarks", script.getTotalMarks());
        
        return ResponseEntity.ok(response);
    }
    
    // Finalize result - sets status to FINALIZED (no further changes allowed)
    @PutMapping("/results/{scriptId}/finalize")
    public ResponseEntity<Map<String, Object>> finalizeResult(
            @PathVariable Long scriptId,
            @RequestParam(required = false) Float finalMarks) {
        
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        
        // Validate that result can be finalized (must be published first)
        if (!"RESULTS_PUBLISHED".equals(script.getStatus()) && 
            !"REVALUATION_COMPLETED".equals(script.getStatus())) {
            throw new RuntimeException("Cannot finalize results. Script is in status: " + script.getStatus());
        }
        
        // Update final marks if provided
        if (finalMarks != null) {
            script.setTotalMarks(finalMarks);
        }
        
        // Set status to FINALIZED (terminal state)
        try {
            AnswerScriptStateMachine.transition(script, "FINALIZED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot finalize results: " + e.getMessage());
        }
        AnswerScript finalizedScript = answerScriptRepository.save(script);
        
        // Notify student about finalization
        if (script.getStudent() != null) {
            notificationService.notifyStudent(script.getStudent(),
                String.format("🏆 Results finalized for Script #%d. Final marks: %.2f (No further changes allowed)", 
                    scriptId, finalizedScript.getTotalMarks()));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Result finalized successfully");
        response.put("scriptId", scriptId);
        response.put("studentId", script.getStudent() != null ? script.getStudent().getUserId() : null);
        response.put("status", "FINALIZED");
        response.put("totalMarks", finalizedScript.getTotalMarks());
        response.put("finalizedAt", java.time.LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
    
    // Bulk publish results for multiple scripts
    @PutMapping("/results/bulk-publish")
    public ResponseEntity<Map<String, Object>> bulkPublishResults(@RequestBody List<Long> scriptIds) {
        int published = 0;
        int failed = 0;
        
        for (Long scriptId : scriptIds) {
            try {
                AnswerScript script = answerScriptRepository.findById(scriptId).orElse(null);
                if (script != null && ("EVALUATED".equals(script.getStatus()) || "REVIEW_COMPLETED".equals(script.getStatus()))) {
                    try {
                        AnswerScriptStateMachine.transition(script, "RESULTS_PUBLISHED");
                    } catch (InvalidStateTransitionException e) {
                        System.err.println("Failed to publish script " + scriptId + ": " + e.getMessage());
                        failed++;
                        continue;
                    }
                    answerScriptRepository.save(script);
                    published++;
                    
                    // Notify student
                    if (script.getStudent() != null) {
                        notificationService.notifyStudent(script.getStudent(),
                            "Results published for Script #" + scriptId);
                    }
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bulk publish completed");
        response.put("published", published);
        response.put("failed", failed);
        
        return ResponseEntity.ok(response);
    }
    
    // Get result statistics
    @GetMapping("/results/stats")
    public ResponseEntity<Map<String, Object>> getResultStats() {
        List<AnswerScript> allScripts = answerScriptRepository.findAll();
        
        long published = allScripts.stream().filter(s -> "RESULTS_PUBLISHED".equals(s.getStatus())).count();
        long finalized = allScripts.stream().filter(s -> "FINALIZED".equals(s.getStatus())).count();
        long evaluated = allScripts.stream().filter(s -> "EVALUATED".equals(s.getStatus())).count();
        long underEvaluation = allScripts.stream().filter(s -> "UNDER_EVALUATION".equals(s.getStatus())).count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalScripts", allScripts.size());
        stats.put("published", published);
        stats.put("finalized", finalized);
        stats.put("evaluated", evaluated);
        stats.put("underEvaluation", underEvaluation);
        
        return ResponseEntity.ok(stats);
    }
    
    // Get dashboard overview
    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        List<ReviewRequest> pendingReviews = reviewService.getPendingReviews();
        List<RevaluationRequest> pendingRevaluations = revaluationService.getPendingRevaluations();
        List<AnswerScript> scriptsUnderEvaluation = answerScriptRepository.findAll().stream()
                .filter(s -> "UNDER_EVALUATION".equals(s.getStatus()))
                .toList();
        
        Map<String, Object> overview = new HashMap<>();
        overview.put("pendingReviews", pendingReviews.size());
        overview.put("pendingRevaluations", pendingRevaluations.size());
        overview.put("scriptsUnderEvaluation", scriptsUnderEvaluation.size());
        
        return ResponseEntity.ok(overview);
    }
}