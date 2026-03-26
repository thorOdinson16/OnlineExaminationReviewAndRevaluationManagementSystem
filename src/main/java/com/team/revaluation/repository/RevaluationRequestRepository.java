// File: src/main/java/com/team/revaluation/repository/RevaluationRequestRepository.java
package com.team.revaluation.repository;

import com.team.revaluation.model.RevaluationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RevaluationRequestRepository extends JpaRepository<RevaluationRequest, Long> {
    List<RevaluationRequest> findByStudentUserId(Long studentId);
    List<RevaluationRequest> findByRevaluationStatus(String status);
    
    // Get requests in REVALUATION_IN_PROGRESS for revaluator
    @Query("SELECT r FROM RevaluationRequest r WHERE r.revaluationStatus = 'REVALUATION_IN_PROGRESS'")
    List<RevaluationRequest> findPendingForRevaluator();
    
    // Get requests by revaluator ID
    List<RevaluationRequest> findByRevaluatorUserId(Long revaluatorId);
    
    // Get requests by revaluator ID and status
    List<RevaluationRequest> findByRevaluatorUserIdAndRevaluationStatus(Long revaluatorId, String status);
    
    // Count requests by status
    @Query("SELECT COUNT(r) FROM RevaluationRequest r WHERE r.revaluationStatus = :status")
    long countByStatus(@Param("status") String status);
}