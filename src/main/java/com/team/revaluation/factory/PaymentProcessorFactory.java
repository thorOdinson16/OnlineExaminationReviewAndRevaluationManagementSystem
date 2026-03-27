package com.team.revaluation.factory;

import com.team.revaluation.service.PaymentProcessor;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory class that uses Abstract Factory pattern.
 * This provides a family of related factories for different payment types.
 */
public class PaymentProcessorFactory {
    
    // Registry of concrete factories (Abstract Factory pattern)
    private static final Map<String, PaymentProcessorAbstractFactory> FACTORY_REGISTRY = new HashMap<>();
    
    static {
        // Register concrete factories
        FACTORY_REGISTRY.put("PARTIAL", new PartialPaymentProcessorFactory());
        FACTORY_REGISTRY.put("FULL", new FullPaymentProcessorFactory());
    }
    
    /**
     * Get the appropriate payment processor using Abstract Factory pattern.
     * This method demonstrates the Factory Method pattern as well.
     */
    public static PaymentProcessor getPaymentProcessor(String paymentType) {
        String type = paymentType.toUpperCase();
        
        PaymentProcessorAbstractFactory factory = FACTORY_REGISTRY.get(type);
        
        if (factory == null) {
            throw new IllegalArgumentException("Invalid payment type: " + paymentType + 
                ". Supported types: PARTIAL, FULL");
        }
        
        // Delegate to the concrete factory to create the processor
        return factory.createPaymentProcessor();
    }
    
    /**
     * Alternative: Direct creation for simplicity
     * (This demonstrates Factory Method pattern)
     */
    public static PaymentProcessor createProcessor(String paymentType) {
        switch (paymentType.toUpperCase()) {
            case "PARTIAL":
                return new com.team.revaluation.service.PartialPaymentProcessor();
            case "FULL":
                return new com.team.revaluation.service.FullPaymentProcessor();
            default:
                throw new IllegalArgumentException("Invalid payment type: " + paymentType);
        }
    }
}