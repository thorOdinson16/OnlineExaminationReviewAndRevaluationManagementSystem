package com.team.revaluation.service;

import org.springframework.stereotype.Component;

@Component
public class PaymentProxy implements IPaymentGateway {

    private IPaymentGateway realGateway;

    public PaymentProxy(IPaymentGateway realGateway) {
        this.realGateway = realGateway;
    }

    @Override
    public boolean processTransaction(Float amount) {
        if (amount == null || amount <= 0) {
            System.err.println("PaymentProxy: Invalid amount: " + amount);
            return false;
        }
        System.out.println("PaymentProxy: Validating payment amount: " + amount);
        return realGateway.processTransaction(amount);
    }
}