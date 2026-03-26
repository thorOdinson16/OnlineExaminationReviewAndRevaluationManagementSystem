package com.team.revaluation.factory;

import com.team.revaluation.service.PaymentProcessor;
import com.team.revaluation.service.PartialPaymentProcessor;
import com.team.revaluation.service.FullPaymentProcessor;

public class PaymentProcessorFactory {
    
    public static PaymentProcessor getPaymentProcessor(String paymentType) {
        switch (paymentType.toUpperCase()) {
            case "PARTIAL":
                return new PartialPaymentProcessor();
            case "FULL":
                return new FullPaymentProcessor();
            default:
                throw new IllegalArgumentException("Invalid payment type: " + paymentType);
        }
    }
}