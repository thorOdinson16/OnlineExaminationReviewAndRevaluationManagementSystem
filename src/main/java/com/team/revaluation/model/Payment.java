package com.team.revaluation.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private Float amount;

    // PARTIAL, FULL
    private String paymentType;

    // PENDING, SUCCESS, FAILED
    private String paymentStatus;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;
}