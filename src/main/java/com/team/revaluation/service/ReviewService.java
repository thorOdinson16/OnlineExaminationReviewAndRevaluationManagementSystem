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

    public ReviewRequest updateReviewStatus(Long reviewId, String status) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        request.setReviewStatus(status);
        return reviewRequestRepository.save(request);
    }
}