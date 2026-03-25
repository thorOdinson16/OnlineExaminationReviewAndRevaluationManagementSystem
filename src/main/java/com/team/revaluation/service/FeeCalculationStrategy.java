package com.team.revaluation.service;

public interface FeeCalculationStrategy {
    Float calculateFee();
}

class ReviewFeeStrategy implements FeeCalculationStrategy {
    public Float calculateFee() { return 500.0f; }
}

class FullRevaluationFeeStrategy implements FeeCalculationStrategy {
    public Float calculateFee() { return 1500.0f; }
}