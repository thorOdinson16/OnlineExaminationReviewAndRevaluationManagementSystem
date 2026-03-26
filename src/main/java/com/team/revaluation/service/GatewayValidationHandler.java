package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.UserRepository;

public class GatewayValidationHandler extends PaymentValidationHandler {
    
    @Override
    public void handle(Payment payment, UserRepository userRepository) {
        // Final validation before gateway call
        System.out.println("GatewayValidationHandler: All validations passed. Ready for gateway.");
        handleNext(payment, userRepository);
    }
}