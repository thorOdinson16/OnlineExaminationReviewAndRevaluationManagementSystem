// File: src/main/java/com/team/revaluation/repository/PaymentRepository.java
package com.team.revaluation.repository;

import com.team.revaluation.model.Payment;
import com.team.revaluation.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Find payments by student
    List<Payment> findByStudent(Student student);
    
    // Find payments by student ID (using JPQL)
    List<Payment> findByStudentUserId(Long studentId);
    
    // Find payments by status
    List<Payment> findByPaymentStatus(String status);
    
    // Find payments by student and status
    List<Payment> findByStudentAndPaymentStatus(Student student, String status);
}