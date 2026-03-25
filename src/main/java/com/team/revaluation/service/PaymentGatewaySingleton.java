package com.team.revaluation.service;

public class PaymentGatewaySingleton implements IPaymentGateway {
    
    // 1. Private static instance of the class
    private static PaymentGatewaySingleton instance;

    // 2. Private constructor to prevent anyone else from using 'new'
    private PaymentGatewaySingleton() {
        System.out.println("Payment Gateway Initialized.");
    }

    // 3. Public static method to get the single instance (Thread-safe)
    public static synchronized PaymentGatewaySingleton getInstance() {
        if (instance == null) {
            instance = new PaymentGatewaySingleton();
        }
        return instance;
    }

    // 4. Override the method from the IPaymentGateway interface
    @Override
    public boolean processTransaction(Float amount) {
        System.out.println("Processing payment of ₹" + amount + " through Singleton Gateway...");
        return true; // Hardcoded to always succeed for the mock
    }
}