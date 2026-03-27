// File: src/main/java/com/team/revaluation/controller/AdminController.java
package com.team.revaluation.controller;

import com.team.revaluation.model.*;
import com.team.revaluation.service.*;
import com.team.revaluation.exception.InvalidStateTransitionException;
import com.team.revaluation.repository.*;
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
    
    @Autowired
    private EvaluatorService evaluatorService;  // ✅ Fixed: Added missing dependency

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
        ReviewRequest verified = reviewService.verifyReview(reviewId);
        return ResponseEntity.ok(verified);
    }

    // ==================== EVALUATOR ASSIGNMENT ====================
    @PostMapping("/evaluator/assign")
    public ResponseEntity<Map<String, Object>> assignEvaluator(
            @RequestParam Long scriptId,
            @RequestParam Long evaluatorId) {

        Map<String, Object> response = evaluatorService.assignEvaluatorToScript(scriptId, evaluatorId);
        return ResponseEntity.ok(response);
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
        
        Map<String, Object> response = revaluationService.assignRevaluatorToRequest(revaluationId, revaluatorId);
        return ResponseEntity.ok(response);
    }
    
    // ==================== FINAL RESULT MANAGEMENT ====================
    
    @PutMapping("/results/{scriptId}/publish")
    public ResponseEntity<Map<String, Object>> publishResult(@PathVariable Long scriptId) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        
        if (!"EVALUATED".equals(script.getStatus()) && !"REVIEW_COMPLETED".equals(script.getStatus())) {
            throw new RuntimeException("Cannot publish results. Script is in status: " + script.getStatus());
        }
        
        try {
            AnswerScriptStateMachine.transition(script, "RESULTS_PUBLISHED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot publish results: " + e.getMessage());
        }
        AnswerScript updatedScript = answerScriptRepository.save(script);
        
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
    
    @PutMapping("/results/{scriptId}/finalize")
    public ResponseEntity<Map<String, Object>> finalizeResult(
            @PathVariable Long scriptId,
            @RequestParam(required = false) Float finalMarks) {
        
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        
        if (!"RESULTS_PUBLISHED".equals(script.getStatus()) && 
            !"REVALUATION_COMPLETED".equals(script.getStatus())) {
            throw new RuntimeException("Cannot finalize results. Script is in status: " + script.getStatus());
        }
        
        if (finalMarks != null) {
            script.setTotalMarks(finalMarks);
        }
        
        try {
            AnswerScriptStateMachine.transition(script, "FINALIZED");
        } catch (InvalidStateTransitionException e) {
            throw new RuntimeException("Cannot finalize results: " + e.getMessage());
        }
        AnswerScript finalizedScript = answerScriptRepository.save(script);
        
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

    // ==================== USER MANAGEMENT ENDPOINTS (NEW) ====================
    
    /**
     * Get all users or filter by role
     * GET /admin/users?role=STUDENT (optional)
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(@RequestParam(required = false) String role) {
        List<User> users;
        if (role != null && !role.isEmpty() && !"ALL".equals(role)) {
            // Filter by role - need to fetch all and filter since JPA doesn't have findByRole
            users = userRepository.findAll().stream()
                    .filter(u -> u.getRole().equals(role))
                    .toList();
        } else {
            users = userRepository.findAll();
        }
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user statistics by role
     * GET /admin/users/stats
     */
    @GetMapping("/users/stats")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        List<User> allUsers = userRepository.findAll();
        
        long students = allUsers.stream().filter(u -> "STUDENT".equals(u.getRole())).count();
        long evaluators = allUsers.stream().filter(u -> "EVALUATOR".equals(u.getRole())).count();
        long revaluators = allUsers.stream().filter(u -> "REVALUATOR".equals(u.getRole())).count();
        long admins = allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("students", students);
        stats.put("evaluators", evaluators);
        stats.put("revaluators", revaluators);
        stats.put("admins", admins);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Delete a user by ID
     * DELETE /admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        userRepository.delete(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully");
        response.put("userId", userId.toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all revaluators (for assignment dropdown)
     * GET /admin/users/revaluators
     */
    @GetMapping("/users/revaluators")
    public ResponseEntity<List<Revaluator>> getRevaluators() {
        List<User> allUsers = userRepository.findAll();
        List<Revaluator> revaluators = allUsers.stream()
                .filter(u -> "REVALUATOR".equals(u.getRole()))
                .map(u -> (Revaluator) u)
                .toList();
        return ResponseEntity.ok(revaluators);
    }
    
    /**
     * Get all evaluators (for assignment dropdown)
     * GET /admin/users/evaluators
     */
    @GetMapping("/users/evaluators")
    public ResponseEntity<List<Evaluator>> getEvaluators() {
        List<User> allUsers = userRepository.findAll();
        List<Evaluator> evaluators = allUsers.stream()
                .filter(u -> "EVALUATOR".equals(u.getRole()))
                .map(u -> (Evaluator) u)
                .toList();
        return ResponseEntity.ok(evaluators);
    }
}