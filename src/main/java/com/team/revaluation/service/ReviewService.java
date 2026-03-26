package com.team.revaluation.service;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.ReviewRequestRepository;
import com.team.revaluation.builder.ReviewRequestBuilder;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Autowired
    @Qualifier("reviewFeeStrategy")
    private FeeCalculationStrategy reviewFeeStrategy;

    @Transactional
    public ReviewRequest applyForReview(ReviewRequest request) {
        ReviewRequest newRequest = new ReviewRequestBuilder()
                .withStudent(request.getStudent())
                .withAnswerScript(request.getAnswerScript())
                .withReviewFee(reviewFeeStrategy.calculateFee())
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

        if ("PAYMENT_SUCCESS".equals(request.getReviewStatus())) {
            throw new RuntimeException("Payment already processed for this review request");
        }

        Payment payment = new Payment();
        payment.setAmount(request.getReviewFee());
        payment.setPaymentType("FULL");
        payment.setPaymentStatus("PENDING");
        payment.setStudent(request.getStudent());

        Payment processedPayment = paymentService.processPayment(payment);

        if ("SUCCESS".equals(processedPayment.getPaymentStatus())) {
            request.setReviewStatus("PAYMENT_SUCCESS");
            
            AnswerScript script = request.getAnswerScript();
            if (script != null) {
                try {
                    com.team.revaluation.service.AnswerScriptStateMachine.transition(script, "REVIEW_REQUESTED");
                } catch (com.team.revaluation.exception.InvalidStateTransitionException e) {
                    throw new RuntimeException("Invalid state transition: " + e.getMessage());
                }
                answerScriptRepository.save(script);
            }
            
            ReviewRequest savedRequest = reviewRequestRepository.save(request);
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

    public List<ReviewRequest> getPendingReviews() {
        return reviewRequestRepository.findByReviewStatus("PAYMENT_PENDING");
    }

    public List<ReviewRequest> getReviewsByStatus(String status) {
        return reviewRequestRepository.findByReviewStatus(status);
    }

    @Transactional
    public ReviewRequest updateReviewStatus(Long reviewId, String newStatus) {
        ReviewRequest request = reviewRequestRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        String currentStatus = request.getReviewStatus();
        
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
                isValidTransition = false;
                break;
            case "VERIFIED":
                isValidTransition = newStatus.equals("IN_PROGRESS");
                break;
            case "REJECTED":
                isValidTransition = false;
                break;
            default:
                isValidTransition = false;
        }
        
        if (!isValidTransition) {
            throw new RuntimeException("Invalid state transition from " + currentStatus + " to " + newStatus);
        }
        
        request.setReviewStatus(newStatus);
        ReviewRequest updatedRequest = reviewRequestRepository.save(request);
        notificationService.notifyReviewStatusChange(updatedRequest);
        
        return updatedRequest;
    }
    
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
    
    @Transactional
    public ReviewRequest verifyReview(Long reviewId) {
        return updateReviewStatus(reviewId, "VERIFIED");
    }
}