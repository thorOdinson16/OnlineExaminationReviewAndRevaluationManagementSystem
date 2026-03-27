package com.team.revaluation.service;

import org.springframework.stereotype.Component;

@Component("reviewFeeStrategy")
public class ReviewFeeStrategy implements FeeCalculationStrategy {
    @Override
    public Float calculateFee() {
        return 500.0f;
    }
}