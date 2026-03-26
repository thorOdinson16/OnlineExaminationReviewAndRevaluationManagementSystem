package com.team.revaluation.service;

import com.team.revaluation.model.Payment;

public class PartialPaymentProcessor implements PaymentProcessor {
    
    @Override
    public boolean process(Payment payment) {
        // Partial payment logic (e.g., 50% advance)
        System.out.println("Processing PARTIAL payment of ₹" + payment.getAmount() + "...");
        // Additional partial payment validation logic
        return true;
    }
}