package com.team.revaluation.service;

import com.team.revaluation.model.Payment;

public interface PaymentProcessor {
    boolean process(Payment payment);
}