package com.team.revaluation.service;

public interface IPaymentGateway {
    boolean processTransaction(Float amount);
}