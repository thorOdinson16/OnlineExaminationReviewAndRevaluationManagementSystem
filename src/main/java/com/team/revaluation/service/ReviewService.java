package com.team.revaluation.service;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.ReviewRequestRepository;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    public ReviewRequest applyForReview(ReviewRequest request) {
        // Use Strategy Pattern to set fee
        FeeCalculationStrategy feeStrategy = new ReviewFeeStrategy();
        request.setReviewFee(feeStrategy.calculateFee());
        request.setReviewStatus("PAYMENT_PENDING");
        return reviewRequestRepository.save(request);
    }

    public ReviewRequest processPaymentForReview(Long reviewId) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review request not found"));

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
        
        ReviewRequest updatedRequest = reviewRequestRepository.save(request);
        
        // Notify about status change (Observer Pattern)
        notificationService.notifyReviewStatusChange(updatedRequest);
        
        return updatedRequest;
    }
}