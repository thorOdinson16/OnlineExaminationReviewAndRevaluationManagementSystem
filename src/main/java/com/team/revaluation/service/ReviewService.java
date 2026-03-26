// File: src/main/java/com/team/revaluation/service/ReviewService.java
package com.team.revaluation.service;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.ReviewRequestRepository;

import com.team.revaluation.builder.ReviewRequestBuilder;

import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ReviewService {

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    
    public ReviewRequest applyForReview(ReviewRequest request) {
        // Use the builder to construct the new request
        ReviewRequest newRequest = new ReviewRequestBuilder()
                .withStudent(request.getStudent())
                .withAnswerScript(request.getAnswerScript())
                .withReviewFee(new ReviewFeeStrategy().calculateFee())
                .withReviewStatus("PAYMENT_PENDING")
                .build();
        return reviewRequestRepository.save(newRequest);
    }

    public ReviewRequest getReviewById(Long reviewId) {
        return reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review request not found with id: " + reviewId));
    }

    @Transactional
    public ReviewRequest processPaymentForReview(Long reviewId) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review request not found"));

        // Check if already paid
        if ("PAYMENT_SUCCESS".equals(request.getReviewStatus())) {
            throw new RuntimeException("Payment already processed for this review request");
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setAmount(request.getReviewFee());
        payment.setPaymentType("FULL");
        payment.setPaymentStatus("PENDING");
        payment.setStudent(request.getStudent());

        // Process payment
        Payment processedPayment = paymentService.processPayment(payment);

        if ("SUCCESS".equals(processedPayment.getPaymentStatus())) {
            request.setReviewStatus("PAYMENT_SUCCESS");
            
            // Update script status to REVIEW_REQUESTED
            AnswerScript script = request.getAnswerScript();
            if (script != null) {
                script.setStatus("REVIEW_REQUESTED");
                answerScriptRepository.save(script);
            }
            
            ReviewRequest savedRequest = reviewRequestRepository.save(request);
            
            // Notify about status change (Observer Pattern)
            notificationService.notifyReviewStatusChange(savedRequest);
            
            return savedRequest;
        } else {
            request.setReviewStatus("PAYMENT_FAILED");
            return reviewRequestRepository.save(request);
        }
    }

    public List<ReviewRequest> getReviewsByStudent(Long studentId) {
        return reviewRequestRepository.findByStudentUserId(studentId);
    }

    public List<ReviewRequest> getAllReviews() {
        return reviewRequestRepository.findAll();
    }

    // Get pending reviews (PAYMENT_PENDING status) for admin verification
    public List<ReviewRequest> getPendingReviews() {
        return reviewRequestRepository.findByReviewStatus("PAYMENT_PENDING");
    }

    // Get reviews with specific status
    public List<ReviewRequest> getReviewsByStatus(String status) {
        return reviewRequestRepository.findByReviewStatus(status);
    }

    @Transactional
    public ReviewRequest updateReviewStatus(Long reviewId, String newStatus) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        String currentStatus = request.getReviewStatus();
        
        // Define valid state transitions
        boolean isValidTransition = false;
        
        switch (currentStatus) {
            case "PAYMENT_PENDING":
                isValidTransition = newStatus.equals("PAYMENT_SUCCESS") || newStatus.equals("PAYMENT_FAILED") || newStatus.equals("CANCELLED");
                break;
            case "PAYMENT_SUCCESS":
                isValidTransition = newStatus.equals("IN_PROGRESS") || newStatus.equals("VERIFIED") || newStatus.equals("REJECTED");
                break;
            case "IN_PROGRESS":
                isValidTransition = newStatus.equals("COMPLETED");
                break;
            case "COMPLETED":
                isValidTransition = false; // Final state, no further transitions
                break;
            case "VERIFIED":
                isValidTransition = newStatus.equals("IN_PROGRESS");
                break;
            case "REJECTED":
                isValidTransition = false; // Final state
                break;
            default:
                isValidTransition = false;
        }
        
        if (!isValidTransition) {
            throw new RuntimeException("Invalid state transition from " + currentStatus + " to " + newStatus);
        }
        
        request.setReviewStatus(newStatus);
        ReviewRequest updatedRequest = reviewRequestRepository.save(request);
        
        // Notify about status change (Observer Pattern)
        notificationService.notifyReviewStatusChange(updatedRequest);
        
        return updatedRequest;
    }
    
    // Cancel a review request (only if payment not yet processed)
    @Transactional
    public ReviewRequest cancelReviewRequest(Long reviewId) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        if (!"PAYMENT_PENDING".equals(request.getReviewStatus())) {
            throw new RuntimeException("Cannot cancel review request in status: " + request.getReviewStatus());
        }
        
        request.setReviewStatus("CANCELLED");
        ReviewRequest updatedRequest = reviewRequestRepository.save(request);
        
        notificationService.notifyStudent(request.getStudent(), 
            "Review request #" + reviewId + " has been cancelled.");
        
        return updatedRequest;
    }
}