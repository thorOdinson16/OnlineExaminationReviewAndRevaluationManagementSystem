package com.team.revaluation.service;

import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.repository.ReviewRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    public ReviewRequest applyForReview(ReviewRequest request) {
        request.setReviewStatus("PAYMENT_PENDING");
        request.setReviewFee(500.0f);
        return reviewRequestRepository.save(request);
    }

    public List<ReviewRequest> getReviewsByStudent(Long studentId) {
        return reviewRequestRepository.findByStudentUserId(studentId);
    }

    public List<ReviewRequest> getAllReviews() {
        return reviewRequestRepository.findAll();
    }

    public ReviewRequest updateReviewStatus(Long reviewId, String newStatus) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        String currentStatus = request.getReviewStatus();
        
        // Validation: Only allow transitioning to VERIFIED or REJECTED if currently PENDING or PAYMENT_SUCCESS
        if ((currentStatus.equals("PENDING") || currentStatus.equals("PAYMENT_SUCCESS")) && 
            (newStatus.equals("VERIFIED") || newStatus.equals("REJECTED"))) {
            request.setReviewStatus(newStatus);
        } else if (newStatus.equals("COMPLETED") && currentStatus.equals("IN_PROGRESS")) {
            request.setReviewStatus(newStatus);
        } else {
            throw new RuntimeException("Invalid state transition from " + currentStatus + " to " + newStatus);
        }
        
        return reviewRequestRepository.save(request);
    }
}

