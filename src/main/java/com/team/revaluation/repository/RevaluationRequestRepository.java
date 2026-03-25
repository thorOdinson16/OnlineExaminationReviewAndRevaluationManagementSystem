// File: src/main/java/com/team/revaluation/repository/RevaluationRequestRepository.java
package com.team.revaluation.repository;

import com.team.revaluation.model.RevaluationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RevaluationRequestRepository extends JpaRepository<RevaluationRequest, Long> {
    List<RevaluationRequest> findByStudentUserId(Long studentId);
    List<RevaluationRequest> findByRevaluationStatus(String status);
}