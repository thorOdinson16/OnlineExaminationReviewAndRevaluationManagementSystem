package com.team.revaluation.service;

import org.springframework.stereotype.Component;

@Component("fullRevaluationFeeStrategy")
public class FullRevaluationFeeStrategy implements FeeCalculationStrategy {
    @Override
    public Float calculateFee() {
        return 1500.0f;
    }
}