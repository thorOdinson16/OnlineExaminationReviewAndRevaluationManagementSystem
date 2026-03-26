package com.team.revaluation.service;

import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.RevaluationRequest;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Component
public class NotificationLogger implements NotificationService.NotificationListener {
    
    private final NotificationService notificationService;
    
    public NotificationLogger(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @PostConstruct
    public void registerListener() {
        // ✅ Register this listener with the NotificationService
        notificationService.addListener(this);
        System.out.println("✅ NotificationLogger registered as observer");
    }
    
    @Override
    public void onReviewStatusChanged(ReviewRequest request) {
        System.out.println(String.format(
            "[OBSERVER LOG - %s] Review Request #%d status changed to: %s",
            LocalDateTime.now(),
            request.getReviewId(),
            request.getReviewStatus()
        ));
    }
    
    @Override
    public void onRevaluationStatusChanged(RevaluationRequest request) {
        System.out.println(String.format(
            "[OBSERVER LOG - %s] Revaluation Request #%d status changed to: %s",
            LocalDateTime.now(),
            request.getRevaluationId(),
            request.getRevaluationStatus()
        ));
    }
}