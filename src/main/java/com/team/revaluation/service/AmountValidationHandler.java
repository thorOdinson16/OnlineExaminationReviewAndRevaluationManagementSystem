package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class AmountValidationHandler extends PaymentValidationHandler {
    
    @Override
    public void handle(Payment payment, UserRepository userRepository) {
        if (payment.getAmount() == null || payment.getAmount() <= 0) {
            throw new RuntimeException("Invalid amount: Amount must be greater than 0");
        }
        System.out.println("AmountValidationHandler: Valid amount ₹" + payment.getAmount());
        handleNext(payment, userRepository);
    }
}