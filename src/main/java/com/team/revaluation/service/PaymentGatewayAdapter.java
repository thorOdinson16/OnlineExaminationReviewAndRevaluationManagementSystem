package com.team.revaluation.service;

/**
 * Adapter that adapts ExternalPaymentGateway to IPaymentGateway interface.
 * This allows us to seamlessly swap external gateway with our internal gateway.
 */
public class PaymentGatewayAdapter implements IPaymentGateway {
    
    private final ExternalPaymentGateway externalGateway;
    
    public PaymentGatewayAdapter() {
        this.externalGateway = new ExternalPaymentGateway();
    }
    
    public PaymentGatewayAdapter(ExternalPaymentGateway externalGateway) {
        this.externalGateway = externalGateway;
    }
    
    @Override
    public boolean processTransaction(Float amount) {
        // Convert Float to double and use INR as currency
        String result = externalGateway.charge(amount.doubleValue(), "INR");
        
        // Return true if transaction ID was returned (success), false otherwise
        return result != null && !result.isEmpty();
    }
}