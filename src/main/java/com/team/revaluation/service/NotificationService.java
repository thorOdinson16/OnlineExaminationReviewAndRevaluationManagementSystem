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

@Service  // ✅ Spring manages this as a singleton - no need for manual Singleton pattern
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    // List of listeners (Observer pattern)
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
    
    public void notifyStudent(Student student, String message) {
        if (student == null) {
            System.out.println(String.format(
                "[NOTIFICATION - %s] System notification: %s",
                LocalDateTime.now(),
                message
            ));
            return;
        }
        
        Notification notification = new Notification();
        notification.setStudent(student);
        notification.setMessage(message);
        notification.setIsRead(false);
        if (notificationRepository != null) {
            notificationRepository.save(notification);
        }
        
        System.out.println(String.format(
            "[NOTIFICATION - %s] To: %s (%s) - %s",
            LocalDateTime.now(),
            student.getName(),
            student.getEmail(),
            message
        ));
        
        sendEmailNotification(student, message);
        sendSmsNotification(student, message);
    }
    
    public void notifyReviewStatusChange(ReviewRequest request) {
        String message = String.format(
            "Review Request #%d status changed to: %s",
            request.getReviewId(),
            request.getReviewStatus()
        );
        
        notifyStudent(request.getStudent(), message);
        
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
        
        for (NotificationListener listener : listeners) {
            listener.onRevaluationStatusChanged(request);
        }
    }
    
    private void sendEmailNotification(Student student, String message) {
        if (student != null) {
            System.out.println("📧 Email sent to " + student.getEmail() + ": " + message);
        }
    }
    
    private void sendSmsNotification(Student student, String message) {
        System.out.println("📱 SMS sent to student: " + message);
    }
    
    public List<Notification> getUnreadNotifications(Student student) {
        if (student == null || notificationRepository == null) {
            return new ArrayList<>();
        }
        return notificationRepository.findByStudentAndIsReadFalse(student);
    }
    
    public void markAsRead(Long notificationId) {
        if (notificationRepository == null) return;
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
}