package com.team.revaluation.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "answer_scripts")
@Data
public class AnswerScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long scriptId;

    // SUBMITTED, UNDER_EVALUATION, EVALUATED, RESULTS_PUBLISHED,
    // REVIEW_REQUESTED, REVIEW_IN_PROGRESS, REVIEW_COMPLETED,
    // REVALUATION_REQUESTED, REVALUATION_IN_PROGRESS, REVALUATION_COMPLETED, FINALIZED
    private String status;

    private Float totalMarks;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;
}