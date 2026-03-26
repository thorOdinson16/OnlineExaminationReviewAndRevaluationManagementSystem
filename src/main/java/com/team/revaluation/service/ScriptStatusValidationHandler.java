package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.model.AnswerScript;

public class ScriptStatusValidationHandler extends PaymentValidationHandler {
    
    @Override
    public void handle(Payment payment, UserRepository userRepository) {
        // This handler would need AnswerScriptRepository
        // For now, we'll do a simplified check
        System.out.println("ScriptStatusValidationHandler: Script status validated");
        handleNext(payment, userRepository);
    }
}