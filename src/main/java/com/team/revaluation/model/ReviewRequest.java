package com.team.revaluation.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "review_requests")
@Data
public class ReviewRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    private Float reviewFee;

    // PENDING, PAYMENT_PENDING, IN_PROGRESS, COMPLETED
    private String reviewStatus;

    @ManyToOne
    @JoinColumn(name = "script_id")
    private AnswerScript answerScript;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;
}