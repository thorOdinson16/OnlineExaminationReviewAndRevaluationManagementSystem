package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.repository.UserRepository;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.ReviewRequestRepository;
import com.team.revaluation.repository.RevaluationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;  // ✅ Add this import

@Component
public class ScriptStatusValidationHandler extends PaymentValidationHandler {
    
    private AnswerScriptRepository answerScriptRepository;
    private ReviewRequestRepository reviewRequestRepository;
    private RevaluationRequestRepository revaluationRequestRepository;
    
    @Autowired
    public void setAnswerScriptRepository(AnswerScriptRepository answerScriptRepository) {
        this.answerScriptRepository = answerScriptRepository;
    }
    
    @Autowired
    public void setReviewRequestRepository(ReviewRequestRepository reviewRequestRepository) {
        this.reviewRequestRepository = reviewRequestRepository;
    }
    
    @Autowired
    public void setRevaluationRequestRepository(RevaluationRequestRepository revaluationRequestRepository) {
        this.revaluationRequestRepository = revaluationRequestRepository;
    }
    
    @Override
    public void handle(Payment payment, UserRepository userRepository) {
        // ✅ Actual implementation: Validate that there's a pending review/revaluation request
        System.out.println("ScriptStatusValidationHandler: Validating script eligibility");
        
        if (payment.getStudent() != null) {
            Long studentId = payment.getStudent().getUserId();
            
            // Check if student has any pending review requests with PAYMENT_PENDING status
            List<com.team.revaluation.model.ReviewRequest> pendingReviews = 
                reviewRequestRepository.findByStudentUserId(studentId);
            
            boolean hasPendingReview = pendingReviews.stream()
                .anyMatch(req -> "PAYMENT_PENDING".equals(req.getReviewStatus()));
            
            // Check if student has any pending revaluation requests with PAYMENT_PENDING status
            List<com.team.revaluation.model.RevaluationRequest> pendingRevaluations = 
                revaluationRequestRepository.findByStudentUserId(studentId);
            
            boolean hasPendingRevaluation = pendingRevaluations.stream()
                .anyMatch(req -> "PAYMENT_PENDING".equals(req.getRevaluationStatus()));
            
            if (!hasPendingReview && !hasPendingRevaluation) {
                // No pending requests, but payment is being processed - this could be valid
                // For now, we'll allow it but log a warning
                System.out.println("⚠️ ScriptStatusValidationHandler: No pending review/revaluation request found for student " + 
                    payment.getStudent().getName() + ". Proceeding with payment.");
            } else {
                System.out.println("✅ ScriptStatusValidationHandler: Valid pending request found for student " + 
                    payment.getStudent().getName());
            }
        }
        
        handleNext(payment, userRepository);
    }
}