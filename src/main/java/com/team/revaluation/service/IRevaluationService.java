// ============================================================
// FILE: src/main/java/com/team/revaluation/service/IRevaluationService.java
// ============================================================
package com.team.revaluation.service;

import com.team.revaluation.model.RevaluationRequest;
import java.util.List;
import java.util.Map;

/**
 * DIP interface for RevaluationService (checklist §5 — Dependency Inversion Principle).
 */
public interface IRevaluationService {
    RevaluationRequest applyForRevaluation(Long scriptId, Long studentId);
    RevaluationRequest getRevaluationById(Long revaluationId);
    List<RevaluationRequest> getRevaluationsByStudent(Long studentId);
    List<RevaluationRequest> getAllRevaluations();
    List<RevaluationRequest> getPendingRevaluations();
    List<RevaluationRequest> getPendingForRevaluator();
    long countByStatus(String status);
    RevaluationRequest processRevaluationPayment(Long revaluationId);
    RevaluationRequest updateRevaluationStatus(Long revaluationId, String newStatus);
    RevaluationRequest rejectRevaluation(Long revaluationId, String reason);
    RevaluationRequest cancelRevaluationRequest(Long revaluationId);
    RevaluationRequest submitRevaluationMarks(Long revaluationId, Float marks, String comments);
    Map<String, Object> assignRevaluatorToRequest(Long revaluationId, Long revaluatorId);
}