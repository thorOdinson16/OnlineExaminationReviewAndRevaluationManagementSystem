package com.team.revaluation.factory;

import com.team.revaluation.service.PaymentProcessor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * PaymentProcessorFactory — Abstract Factory pattern (Creational, checklist §4.1).
 *
 * PATTERN EXPLANATION for report:
 *   - PaymentProcessorAbstractFactory is the abstract factory interface.
 *   - FullPaymentProcessorFactory and PartialPaymentProcessorFactory are the
 *     two concrete factories, each encapsulating the creation of one processor.
 *   - PaymentProcessorFactory is the CLIENT of those factories: it holds a
 *     registry of PaymentProcessorAbstractFactory instances and delegates
 *     creation to them.
 *
 * This is a Spring bean (@Component) so PaymentService can @Autowire it,
 * removing the previous static method call that bypassed the interface.
 */
@Component
public class PaymentProcessorFactory {

    // Registry: payment type → concrete abstract-factory implementation
    private final Map<String, PaymentProcessorAbstractFactory> registry = new HashMap<>();

    public PaymentProcessorFactory() {
        // Register the two concrete factories
        registry.put("PARTIAL", new PartialPaymentProcessorFactory());
        registry.put("FULL",    new FullPaymentProcessorFactory());
    }

    /**
     * Returns the correct PaymentProcessor by delegating to the registered
     * concrete factory.  The client (PaymentService) depends only on the
     * PaymentProcessorAbstractFactory interface — never on a concrete class.
     *
     * @param paymentType "PARTIAL" or "FULL"
     * @return a PaymentProcessor instance created by the matching factory
     */
    public PaymentProcessor getPaymentProcessor(String paymentType) {
        String key = (paymentType == null) ? "" : paymentType.toUpperCase();

        PaymentProcessorAbstractFactory factory = registry.get(key);
        if (factory == null) {
            throw new IllegalArgumentException(
                "No factory registered for payment type: " + paymentType +
                ". Supported: PARTIAL, FULL");
        }

        // Delegate creation to the concrete abstract factory
        return factory.createPaymentProcessor();
    }
}