package com.team.revaluation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "revaluators")
@Data
@EqualsAndHashCode(callSuper = true)
public class Revaluator extends User {

    private String specialization;
}