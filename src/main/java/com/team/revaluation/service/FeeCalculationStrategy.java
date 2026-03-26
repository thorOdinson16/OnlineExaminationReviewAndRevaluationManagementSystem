package com.team.revaluation.service;

import org.springframework.stereotype.Component;

public interface FeeCalculationStrategy {
    Float calculateFee();
}

@Component("reviewFeeStrategy")
class ReviewFeeStrategy implements FeeCalculationStrategy {
    public Float calculateFee() { return 500.0f; }
}

@Component("fullRevaluationFeeStrategy")
class FullRevaluationFeeStrategy implements FeeCalculationStrategy {
    public Float calculateFee() { return 1500.0f; }
}