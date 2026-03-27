package com.team.revaluation.service;

import com.team.revaluation.model.Student;
import org.springframework.stereotype.Component;

@Component
public class PaymentProxy implements IPaymentGateway {

    private IPaymentGateway realGateway;

    // No-arg constructor - create the real gateway internally
    public PaymentProxy() {
        this.realGateway = PaymentGatewaySingleton.getInstance();
    }
    
    // Keep the parameterized constructor for testing if needed
    public PaymentProxy(IPaymentGateway realGateway) {
        this.realGateway = realGateway;
    }
    
    // Store student reference for validation
    private Student currentStudent;

    /**
     * Set the student for validation - called before payment processing
     */
    public void setStudent(Student student) {
        this.currentStudent = student;
    }

    @Override
    public boolean processTransaction(Float amount) {
        // ✅ Validate amount > 0
        if (amount == null || amount <= 0) {
            System.err.println("PaymentProxy: Invalid amount: " + amount);
            return false;
        }
        
        // ✅ Validate student exists
        if (currentStudent == null) {
            System.err.println("PaymentProxy: No student associated with payment");
            return false;
        }
        
        if (currentStudent.getUserId() == null) {
            System.err.println("PaymentProxy: Invalid student - missing ID");
            return false;
        }
        
        System.out.println("PaymentProxy: Validating payment - Amount: ₹" + amount + 
                          ", Student: " + currentStudent.getName() + " (ID: " + currentStudent.getUserId() + ")");
        
        return realGateway.processTransaction(amount);
    }
}