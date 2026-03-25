package com.team.revaluation.service;

import java.time.LocalDateTime;

public class PaymentLoggingDecorator implements IPaymentGateway {
    
    private final IPaymentGateway wrappedGateway;

    public PaymentLoggingDecorator(IPaymentGateway gateway) {
        this.wrappedGateway = gateway;
    }

    @Override
    public boolean processTransaction(Float amount) {
        // Added behavior: Logging BEFORE the transaction
        System.out.println("[AUDIT LOG - " + LocalDateTime.now() + "] Initiating payment for ₹" + amount);
        
        // Delegate to the actual gateway
        boolean result = wrappedGateway.processTransaction(amount);
        
        // Added behavior: Logging AFTER the transaction
        System.out.println("[AUDIT LOG - " + LocalDateTime.now() + "] Payment result: " + (result ? "SUCCESS" : "FAILED"));
        return result;
    }
}