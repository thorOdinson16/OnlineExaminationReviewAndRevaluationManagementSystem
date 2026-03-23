package com.team.revaluation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "evaluators")
@Data
@EqualsAndHashCode(callSuper = true)
public class Evaluator extends User {

    private String department;
}