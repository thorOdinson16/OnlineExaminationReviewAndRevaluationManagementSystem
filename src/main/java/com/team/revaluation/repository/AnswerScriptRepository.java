package com.team.revaluation.repository;

import com.team.revaluation.model.AnswerScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnswerScriptRepository extends JpaRepository<AnswerScript, Long> {
    List<AnswerScript> findByStudentUserId(Long studentId);
    
    // Find all scripts by status
    List<AnswerScript> findByStatus(String status);
    
    // Find scripts that can be published (evaluated or review completed)
    @Query("SELECT a FROM AnswerScript a WHERE a.status IN ('EVALUATED', 'REVIEW_COMPLETED')")
    List<AnswerScript> findPublishableScripts();
    
    // Find scripts that can be finalized
    @Query("SELECT a FROM AnswerScript a WHERE a.status IN ('RESULTS_PUBLISHED', 'REVALUATION_COMPLETED')")
    List<AnswerScript> findFinalizableScripts();
    
    // Count scripts by status
    @Query("SELECT COUNT(a) FROM AnswerScript a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);
    
    // Get all scripts with student details
    @Query("SELECT a FROM AnswerScript a LEFT JOIN FETCH a.student LEFT JOIN FETCH a.exam")
    List<AnswerScript> findAllWithDetails();
}