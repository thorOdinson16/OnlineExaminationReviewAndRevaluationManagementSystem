package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.UserRepository;

public abstract class PaymentValidationHandler {
    
    protected PaymentValidationHandler next;
    
    public void setNext(PaymentValidationHandler next) {
        this.next = next;
    }
    
    public abstract void handle(Payment payment, UserRepository userRepository);
    
    protected void handleNext(Payment payment, UserRepository userRepository) {
        if (next != null) {
            next.handle(payment, userRepository);
        }
    }
}