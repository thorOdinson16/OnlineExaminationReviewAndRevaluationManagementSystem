package com.team.revaluation.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "revaluation_requests")
@Data
public class RevaluationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long revaluationId;

    private Float revaluationFee;

    // PAYMENT_PENDING, PAYMENT_SUCCESS, PAYMENT_FAILED, VERIFIED, 
    // REVALUATION_IN_PROGRESS, REVALUATION_COMPLETED, CANCELLED, REJECTED
    private String revaluationStatus;

    @ManyToOne
    @JoinColumn(name = "script_id")
    private AnswerScript answerScript;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "revaluator_id")
    private Revaluator revaluator;

    private LocalDateTime appliedDate;
    private LocalDateTime completedDate;
    private String rejectionReason;

    @PrePersist
    protected void onCreate() {
        appliedDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        if ("REVALUATION_COMPLETED".equals(revaluationStatus) || "CANCELLED".equals(revaluationStatus) || "REJECTED".equals(revaluationStatus)) {
            completedDate = LocalDateTime.now();
        }
    }
}