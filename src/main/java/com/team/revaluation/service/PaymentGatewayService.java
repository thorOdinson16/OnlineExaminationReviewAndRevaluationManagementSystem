// File: src/main/java/com/team/revaluation/service/PaymentGatewayService.java
package com.team.revaluation.service;

import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {
    
    private IPaymentGateway gateway;
    private boolean useExternalGateway = false; // Toggle this to switch gateways
    
    public PaymentGatewayService() {
        // Demonstrate adapter pattern by swapping gateways
        if (useExternalGateway) {
            this.gateway = new PaymentGatewayAdapter();
            System.out.println("Using External Payment Gateway (via Adapter)");
        } else {
            this.gateway = PaymentGatewaySingleton.getInstance();
            System.out.println("Using Internal Payment Gateway (Singleton)");
        }
    }
    
    public void switchToExternalGateway() {
        this.gateway = new PaymentGatewayAdapter();
        this.useExternalGateway = true;
        System.out.println("Switched to External Payment Gateway");
    }
    
    public void switchToInternalGateway() {
        this.gateway = PaymentGatewaySingleton.getInstance();
        this.useExternalGateway = false;
        System.out.println("Switched to Internal Payment Gateway");
    }
    
    public IPaymentGateway getGateway() {
        return gateway;
    }
    
    public boolean processTransaction(Float amount) {
        return gateway.processTransaction(amount);
    }
}