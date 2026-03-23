package com.team.revaluation.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "revaluation_requests")
@Data
public class RevaluationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long revaluationId;

    private Float revaluationFee;

    // PENDING, PAYMENT_PENDING, IN_PROGRESS, COMPLETED
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
}