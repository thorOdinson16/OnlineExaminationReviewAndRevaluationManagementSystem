package com.team.revaluation.service;

import java.util.Random;

/**
 * Fake external payment gateway with a different method signature.
 * This represents a third-party service that we cannot modify.
 */
public class ExternalPaymentGateway {
    
    private static final Random random = new Random();
    
    /**
     * External gateway uses 'charge' method with double amount.
     * @param amount the amount to charge
     * @param currency the currency code (e.g., "INR", "USD")
     * @return transaction ID if successful, null if failed
     */
    public String charge(double amount, String currency) {
        System.out.println("ExternalPaymentGateway: Charging " + currency + " " + amount);
        
        // Simulate processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 90% success rate
        if (random.nextInt(100) < 90) {
            String transactionId = "TXN" + System.currentTimeMillis() + random.nextInt(10000);
            System.out.println("ExternalPaymentGateway: Transaction successful. ID: " + transactionId);
            return transactionId;
        } else {
            System.out.println("ExternalPaymentGateway: Transaction failed.");
            return null;
        }
    }
}