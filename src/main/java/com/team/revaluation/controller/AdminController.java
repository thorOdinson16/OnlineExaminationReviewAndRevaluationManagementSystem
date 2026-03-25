// File: src/main/java/com/team/revaluation/controller/AdminController.java
package com.team.revaluation.controller;

import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.service.ReviewService;
import com.team.revaluation.service.RevaluationService;
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
    
    // Assign evaluator to script
    @PostMapping("/evaluator/assign")
    public ResponseEntity<Map<String, Object>> assignEvaluator(
            @RequestParam Long scriptId,
            @RequestParam Long evaluatorId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Evaluator assigned successfully");
        response.put("scriptId", scriptId);
        response.put("evaluatorId", evaluatorId);
        return ResponseEntity.ok(response);
    }
    
    // Get all revaluation requests
    @GetMapping("/revaluations")
    public ResponseEntity<List<com.team.revaluation.model.RevaluationRequest>> getAllRevaluations() {
        return ResponseEntity.ok(revaluationService.getAllRevaluations());
    }
    
    // Verify revaluation request
    @PutMapping("/revaluations/{id}/verify")
    public ResponseEntity<com.team.revaluation.model.RevaluationRequest> verifyRevaluation(
            @PathVariable Long id) {
        return ResponseEntity.ok(revaluationService.updateRevaluationStatus(id, "VERIFIED"));
    }
}