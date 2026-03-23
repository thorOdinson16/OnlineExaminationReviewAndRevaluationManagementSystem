package com.team.revaluation.repository;

import com.team.revaluation.model.RevaluationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevaluationRequestRepository extends JpaRepository<RevaluationRequest, Long> {}