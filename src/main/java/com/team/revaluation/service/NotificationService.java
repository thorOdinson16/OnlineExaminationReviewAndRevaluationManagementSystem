package com.team.revaluation.service;

import com.team.revaluation.model.ReviewRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {
    
    // Singleton pattern using Spring's singleton scope
    private List<NotificationListener> listeners = new ArrayList<>();
    
    public interface NotificationListener {
        void onReviewStatusChanged(ReviewRequest request);
    }
    
    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }
    
    public void notifyReviewStatusChange(ReviewRequest request) {
        String message = String.format(
            "[NOTIFICATION - %s] Review Request #%d status changed to: %s",
            LocalDateTime.now(),
            request.getReviewId(),
            request.getReviewStatus()
        );
        
        System.out.println(message);
        
        // Notify all registered listeners
        for (NotificationListener listener : listeners) {
            listener.onReviewStatusChanged(request);
        }
        
        // Simulate email notification
        sendEmailNotification(request);
        
        // Simulate SMS notification
        sendSmsNotification(request);
    }
    
    private void sendEmailNotification(ReviewRequest request) {
        System.out.println("📧 Email sent to student: Review request #" + request.getReviewId() + 
                          " is now " + request.getReviewStatus());
    }
    
    private void sendSmsNotification(ReviewRequest request) {
        System.out.println("📱 SMS sent: Review #" + request.getReviewId() + 
                          " status: " + request.getReviewStatus());
    }
}