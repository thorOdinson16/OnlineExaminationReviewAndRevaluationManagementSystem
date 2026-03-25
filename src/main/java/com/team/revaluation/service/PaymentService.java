package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public Payment processPayment(Payment payment) {
        // Grab the single instance of your new gateway
        PaymentGatewaySingleton gateway = PaymentGatewaySingleton.getInstance();
        
        // Process the transaction using the mock logic
        boolean isSuccess = gateway.processTransaction(payment.getAmount());
        
        // Update the database status based on the result
        if (isSuccess) {
            payment.setPaymentStatus("SUCCESS");
        } else {
            payment.setPaymentStatus("FAILED");
        }
        
        return paymentRepository.save(payment);
    }
}