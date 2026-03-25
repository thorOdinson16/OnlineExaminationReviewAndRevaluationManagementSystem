// File: src/main/java/com/team/revaluation/service/PaymentService.java
package com.team.revaluation.service;

import com.team.revaluation.model.Notification;
import com.team.revaluation.model.Payment;
import com.team.revaluation.model.Student;
import com.team.revaluation.repository.NotificationRepository;
import com.team.revaluation.repository.PaymentRepository;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private IPaymentGateway paymentGateway;
    
    public PaymentService() {
        // Use decorator pattern - wrap the singleton gateway with logging decorator
        IPaymentGateway gateway = PaymentGatewaySingleton.getInstance();
        this.paymentGateway = new PaymentLoggingDecorator(gateway);
    }

    public Payment processPayment(Payment payment) {
        // Validate amount
        if (payment.getAmount() == null || payment.getAmount() <= 0) {
            payment.setPaymentStatus("FAILED");
            payment.setPaymentStatus("INVALID_AMOUNT");
            return paymentRepository.save(payment);
        }
        
        // Validate student exists
        if (payment.getStudent() == null || payment.getStudent().getUserId() == null) {
            payment.setPaymentStatus("FAILED");
            payment.setPaymentStatus("INVALID_STUDENT");
            return paymentRepository.save(payment);
        }
        
        // Process the transaction using the decorated gateway
        boolean isSuccess = paymentGateway.processTransaction(payment.getAmount());
        
        // Update the database status based on the result
        if (isSuccess) {
            payment.setPaymentStatus("SUCCESS");
        } else {
            payment.setPaymentStatus("FAILED");
        }
        
        return paymentRepository.save(payment);
    }
    
    // Get payment by ID
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
    }
    
    // Get all payments for a student
    public List<Payment> getPaymentsByStudent(Long studentId) {
        Student student = (Student) userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        // This assumes you have a method in PaymentRepository to find by student
        // If not, we need to add it to the repository
        return paymentRepository.findByStudent(student);
    }
    
    // Get all payments
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
    
    // Get payments by status
    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByPaymentStatus(status);
    }
    
    // Get unread notifications for a student
    public List<Notification> getUnreadNotifications(Long studentId) {
        Student student = (Student) userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return notificationRepository.findByStudentAndIsReadFalse(student);
    }
    
    // Mark notification as read
    public void markNotificationAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    // Get all notifications for a student
    public List<Notification> getNotificationsByStudent(Long studentId) {
        Student student = (Student) userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return notificationRepository.findByStudent(student);
    }
}