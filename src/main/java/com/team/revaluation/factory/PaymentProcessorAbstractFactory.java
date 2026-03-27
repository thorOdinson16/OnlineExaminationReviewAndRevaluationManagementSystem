package com.team.revaluation.factory;

import com.team.revaluation.service.PaymentProcessor;

/**
 * Abstract Factory interface for creating payment processors.
 * This demonstrates the Abstract Factory pattern.
 */
public interface PaymentProcessorAbstractFactory {
    PaymentProcessor createPaymentProcessor();
}

/**
 * Concrete factory for Full Payment Processor
 */
class FullPaymentProcessorFactory implements PaymentProcessorAbstractFactory {
    @Override
    public PaymentProcessor createPaymentProcessor() {
        return new com.team.revaluation.service.FullPaymentProcessor();
    }
}

/**
 * Concrete factory for Partial Payment Processor
 */
class PartialPaymentProcessorFactory implements PaymentProcessorAbstractFactory {
    @Override
    public PaymentProcessor createPaymentProcessor() {
        return new com.team.revaluation.service.PartialPaymentProcessor();
    }
}