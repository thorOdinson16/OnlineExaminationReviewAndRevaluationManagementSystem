package com.team.revaluation.factory;

import com.team.revaluation.model.*;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    public User createUser(String role, String name, String email, String password) {
        User user;
        
        switch (role.toUpperCase()) {
            case "STUDENT":
                user = new Student();
                break;
            case "EVALUATOR":
                user = new Evaluator();
                break;
            case "REVALUATOR":
                user = new Revaluator();
                break;
            case "ADMIN":
                user = new Admin();
                break;
            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }
        
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role.toUpperCase());
        
        return user;
    }
}