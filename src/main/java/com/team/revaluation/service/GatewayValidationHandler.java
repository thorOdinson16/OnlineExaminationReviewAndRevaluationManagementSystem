package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class GatewayValidationHandler extends PaymentValidationHandler {
    
    @Override
    public void handle(Payment payment, UserRepository userRepository) {
        // Final validation before gateway call
        System.out.println("GatewayValidationHandler: All validations passed. Ready for gateway.");
        
        // Additional validation - ensure payment amount is reasonable
        if (payment.getAmount() > 100000) { // ₹1,00,000 limit
            throw new RuntimeException("Payment amount exceeds maximum limit");
        }
        
        handleNext(payment, userRepository);
    }
}