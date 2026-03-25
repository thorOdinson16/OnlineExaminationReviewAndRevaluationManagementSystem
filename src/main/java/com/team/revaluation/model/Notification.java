// File: src/main/java/com/team/revaluation/model/Notification.java
package com.team.revaluation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private String message;

    private LocalDateTime createdAt;

    private Boolean isRead = false;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}