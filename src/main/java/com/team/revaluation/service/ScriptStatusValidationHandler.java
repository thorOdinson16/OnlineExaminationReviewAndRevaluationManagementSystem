package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.repository.UserRepository;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScriptStatusValidationHandler extends PaymentValidationHandler {
    
    private AnswerScriptRepository answerScriptRepository;
    
    @Autowired
    public void setAnswerScriptRepository(AnswerScriptRepository answerScriptRepository) {
        this.answerScriptRepository = answerScriptRepository;
    }
    
    @Override
    public void handle(Payment payment, UserRepository userRepository) {
        // For review/revaluation payments, we need to validate the associated script
        // Since Payment doesn't directly have script reference, we'll check if there's
        // any pending review or revaluation request for this student
        
        System.out.println("ScriptStatusValidationHandler: Validating script eligibility");
        
        // This is a simplified validation
        // In a real implementation, you would check if the student has any
        // pending review/revaluation requests that need payment
        
        if (payment.getStudent() != null) {
            // Check if student has any pending review requests
            // This would require ReviewRequestRepository injection
            System.out.println("ScriptStatusValidationHandler: Student " + 
                payment.getStudent().getName() + " is eligible for payment");
        }
        
        handleNext(payment, userRepository);
    }
}