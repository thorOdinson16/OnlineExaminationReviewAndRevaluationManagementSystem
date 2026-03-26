package com.team.revaluation.service;

import com.team.revaluation.model.Payment;

public class FullPaymentProcessor implements PaymentProcessor {
    
    @Override
    public boolean process(Payment payment) {
        // Full payment logic
        System.out.println("Processing FULL payment of ₹" + payment.getAmount() + "...");
        // Additional full payment validation logic
        return true;
    }
}
