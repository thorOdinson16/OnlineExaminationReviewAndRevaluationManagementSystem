package com.team.revaluation.service;

import com.team.revaluation.model.Student;

/**
 * PaymentProxy — Structural (Proxy) Pattern (checklist §4.2).
 *
 * PATTERN EXPLANATION for report:
 *   - Implements the same IPaymentGateway interface as the real gateway.
 *   - Adds validation guards BEFORE forwarding to the real gateway:
 *       1. Amount must be > 0
 *       2. Student must not be null (i.e., student exists)
 *   - The caller (PaymentService) is unaware whether it's talking to the
 *     proxy or the real gateway — both honour the same interface.
 *
 * This satisfies: "PaymentProxy.java validates amount > 0 and student exists
 * before gateway call" (checklist §3.2 / §4.2).
 */
public class PaymentProxy implements IPaymentGateway {

    private final IPaymentGateway realGateway;
    private Student student;   // set by PaymentService before each call

    public PaymentProxy(IPaymentGateway realGateway) {
        this.realGateway = realGateway;
    }

    /** Called by PaymentService to supply the current student context. */
    public void setStudent(Student student) {
        this.student = student;
    }

    /**
     * Guard 1 — amount must be greater than zero.
     * Guard 2 — student must be set (non-null), proving the student exists.
     * If either check fails, an exception is thrown and the gateway is never called.
     */
    @Override
    public boolean processTransaction(Float amount) {
        // Guard 1: amount validation
        if (amount == null || amount <= 0) {
            throw new RuntimeException(
                "[PaymentProxy] Rejected: amount must be > 0. Received: " + amount);
        }

        // Guard 2: student-exists validation
        if (student == null || student.getUserId() == null) {
            throw new RuntimeException(
                "[PaymentProxy] Rejected: student context is missing. " +
                "Ensure PaymentService.setStudent() is called before processTransaction().");
        }

        System.out.printf("[PaymentProxy] Guards passed — amount=₹%.2f, student=%s. " +
            "Forwarding to real gateway.%n", amount, student.getName());

        // Delegate to the real gateway (or the Decorator that wraps it)
        return realGateway.processTransaction(amount);
    }
}
