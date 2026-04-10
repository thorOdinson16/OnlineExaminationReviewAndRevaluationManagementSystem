// ============================================================
// FILE: src/main/java/com/team/revaluation/service/IReviewService.java
// ============================================================
package com.team.revaluation.service;

import com.team.revaluation.model.ReviewRequest;
import java.util.List;

/**
 * DIP interface for ReviewService (checklist §5 — Dependency Inversion Principle).
 * Controllers and Facade depend on this abstraction, not on the concrete class.
 */
public interface IReviewService {
    ReviewRequest applyForReview(ReviewRequest request);
    ReviewRequest getReviewById(Long reviewId);
    List<ReviewRequest> getReviewsByStudent(Long studentId);
    List<ReviewRequest> getAllReviews();
    List<ReviewRequest> getPendingReviews();
    List<ReviewRequest> getReviewsByStatus(String status);
    ReviewRequest processPaymentForReview(Long reviewId);
    ReviewRequest updateReviewStatus(Long reviewId, String newStatus);
    ReviewRequest cancelReviewRequest(Long reviewId);
    ReviewRequest verifyReview(Long reviewId);
}