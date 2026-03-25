// File: src/main/java/com/team/revaluation/service/NotificationService.java
package com.team.revaluation.service;

import com.team.revaluation.model.Notification;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    // Singleton pattern using Spring's singleton scope
    private List<NotificationListener> listeners = new ArrayList<>();
    
    public interface NotificationListener {
        void onReviewStatusChanged(ReviewRequest request);
        void onRevaluationStatusChanged(RevaluationRequest request);
    }
    
    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }
    
    // Method to persist notification to database
    public void notifyStudent(Student student, String message) {
        Notification notification = new Notification();
        notification.setStudent(student);
        notification.setMessage(message);
        notification.setIsRead(false);
        notificationRepository.save(notification);
        
        // Also print to console for demonstration
        System.out.println(String.format(
            "[NOTIFICATION - %s] To: %s (%s) - %s",
            LocalDateTime.now(),
            student.getName(),
            student.getEmail(),
            message
        ));
        
        // Simulate email notification
        sendEmailNotification(student, message);
        
        // Simulate SMS notification
        sendSmsNotification(student, message);
    }
    
    public void notifyReviewStatusChange(ReviewRequest request) {
        String message = String.format(
            "Review Request #%d status changed to: %s",
            request.getReviewId(),
            request.getReviewStatus()
        );
        
        notifyStudent(request.getStudent(), message);
        
        // Notify all registered listeners
        for (NotificationListener listener : listeners) {
            listener.onReviewStatusChanged(request);
        }
    }
    
    public void notifyRevaluationStatusChange(RevaluationRequest request) {
        String message = String.format(
            "Revaluation Request #%d status changed to: %s",
            request.getRevaluationId(),
            request.getRevaluationStatus()
        );
        
        notifyStudent(request.getStudent(), message);
        
        // Notify all registered listeners
        for (NotificationListener listener : listeners) {
            listener.onRevaluationStatusChanged(request);
        }
    }
    
    private void sendEmailNotification(Student student, String message) {
        System.out.println("📧 Email sent to " + student.getEmail() + ": " + message);
    }
    
    private void sendSmsNotification(Student student, String message) {
        System.out.println("📱 SMS sent to student: " + message);
    }
    
    // Get unread notifications for a student
    public List<Notification> getUnreadNotifications(Student student) {
        return notificationRepository.findByStudentAndIsReadFalse(student);
    }
    
    // Mark notification as read
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}