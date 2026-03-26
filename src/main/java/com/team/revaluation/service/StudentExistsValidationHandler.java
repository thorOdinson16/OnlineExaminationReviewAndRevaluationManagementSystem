package com.team.revaluation.service;

import com.team.revaluation.model.Payment;
import com.team.revaluation.model.Student;
import com.team.revaluation.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class StudentExistsValidationHandler extends PaymentValidationHandler {
    
    @Override
    public void handle(Payment payment, UserRepository userRepository) {
        if (payment.getStudent() == null || payment.getStudent().getUserId() == null) {
            throw new RuntimeException("Invalid student: Student information is missing");
        }
        
        Student student = (Student) userRepository.findById(payment.getStudent().getUserId())
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + payment.getStudent().getUserId()));
        
        payment.setStudent(student);
        System.out.println("StudentExistsValidationHandler: Student found - " + student.getName());
        handleNext(payment, userRepository);
    }
}