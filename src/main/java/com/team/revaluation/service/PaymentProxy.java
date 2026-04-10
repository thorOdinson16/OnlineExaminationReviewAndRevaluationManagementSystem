package com.team.revaluation.service;

import com.team.revaluation.model.Student;
import com.team.revaluation.model.User;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * PaymentProxy — Structural (Proxy) Pattern.
 *
 * Validates:
 *   1. amount > 0
 *   2. student object is not null
 *   3. student actually exists in the database (DB re-query)
 * before forwarding to the real gateway.
 */
@Component
public class PaymentProxy implements IPaymentGateway {

    private IPaymentGateway realGateway;

    @Autowired
    private UserRepository userRepository;

    // No-arg constructor - Spring will inject userRepository via @Autowired field
    public PaymentProxy() {
        this.realGateway = PaymentGatewaySingleton.getInstance();
    }

    // Parameterised constructor for testing
    public PaymentProxy(IPaymentGateway realGateway) {
        this.realGateway = realGateway;
    }

    // Store student reference for validation — set by PaymentService before each call
    private Student currentStudent;

    public void setStudent(Student student) {
        this.currentStudent = student;
    }

    @Override
    public boolean processTransaction(Float amount) {
        // Validation 1: amount must be positive
        if (amount == null || amount <= 0) {
            System.err.println("PaymentProxy: Rejected — invalid amount: " + amount);
            return false;
        }

        // Validation 2: student object must be present
        if (currentStudent == null) {
            System.err.println("PaymentProxy: Rejected — no student associated with payment");
            return false;
        }

        if (currentStudent.getUserId() == null) {
            System.err.println("PaymentProxy: Rejected — student has no ID");
            return false;
        }

        // Validation 3: student must actually exist in the database
        if (userRepository != null) {
            Optional<User> dbUser = userRepository.findById(currentStudent.getUserId());
            if (dbUser.isEmpty()) {
                System.err.println("PaymentProxy: Rejected — student ID " +
                        currentStudent.getUserId() + " does not exist in DB");
                return false;
            }
            System.out.println("PaymentProxy: DB check passed — student " +
                    dbUser.get().getName() + " (ID: " + currentStudent.getUserId() + ") exists");
        } else {
            // Fallback if Spring hasn't injected repository (e.g. unit test via constructor)
            System.out.println("PaymentProxy: UserRepository not injected — skipping DB check");
        }

        System.out.println("PaymentProxy: All validations passed — forwarding ₹" + amount + " to gateway");
        return realGateway.processTransaction(amount);
    }
}