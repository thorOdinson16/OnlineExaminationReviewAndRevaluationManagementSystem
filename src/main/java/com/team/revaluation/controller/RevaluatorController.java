package com.team.revaluation.controller;

import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.Notification;
import com.team.revaluation.repository.RevaluationRequestRepository;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/revaluator")
public class RevaluatorController {

    @Autowired
    private RevaluationRequestRepository revaluationRequestRepository;
    
    @Autowired
    private AnswerScriptRepository answerScriptRepository;
    
    @Autowired
    private NotificationService notificationService;

    // Get all revaluation requests (filtered to REVALUATION_IN_PROGRESS only)
    @GetMapping("/requests")
    public ResponseEntity<List<RevaluationRequest>> getAllRequests() {
        List<RevaluationRequest> requests = revaluationRequestRepository.findByRevaluationStatus("REVALUATION_IN_PROGRESS");
        return ResponseEntity.ok(requests);
    }
    
    // Get pending revaluation requests for revaluator
    @GetMapping("/requests/pending")
    public ResponseEntity<List<RevaluationRequest>> getPendingRequests() {
        List<RevaluationRequest> requests = revaluationRequestRepository.findByRevaluationStatus("REVALUATION_IN_PROGRESS");
        return ResponseEntity.ok(requests);
    }
    
    // Get specific revaluation request by ID
    @GetMapping("/requests/{id}")
    public ResponseEntity<RevaluationRequest> getRequestById(@PathVariable Long id) {
        RevaluationRequest request = revaluationRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revaluation request not found"));
        return ResponseEntity.ok(request);
    }

    // Submit revaluation marks - Updates to REVALUATION_COMPLETED using State Machine
    @PutMapping("/requests/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitRevaluationMarks(
            @PathVariable Long id,
            @RequestParam Float marks,
            @RequestParam(required = false) String comments) {
        
        RevaluationRequest request = revaluationRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        
        // Check if request is in correct state
        if (!"REVALUATION_IN_PROGRESS".equals(request.getRevaluationStatus())) {
            throw new RuntimeException("Cannot submit marks. Request is in status: " + request.getRevaluationStatus());
        }
        
        // Update the answer script with new marks
        AnswerScript script = request.getAnswerScript();
        Float oldMarks = script.getTotalMarks();
        script.setTotalMarks(marks);
        
        // ✅ Use state machine instead of direct setStatus
        try {
            com.team.revaluation.service.AnswerScriptStateMachine.transition(script, "REVALUATION_COMPLETED");
        } catch (com.team.revaluation.exception.InvalidStateTransitionException e) {
            throw new RuntimeException("Invalid state transition: " + e.getMessage());
        }
        answerScriptRepository.save(script);
        
        // Update request status to REVALUATION_COMPLETED (per state diagram)
        request.setRevaluationStatus("REVALUATION_COMPLETED");
        RevaluationRequest savedRequest = revaluationRequestRepository.save(request);
        
        // Notify student about revaluation completion (Observer Pattern)
        notificationService.notifyStudent(request.getStudent(), 
            String.format("✅ Revaluation completed for Script #%d. Marks updated from %.2f to %.2f. %s",
                script.getScriptId(), oldMarks, marks, 
                comments != null ? "Comments: " + comments : ""));
        notificationService.notifyRevaluationStatusChange(savedRequest);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Revaluation marks submitted successfully");
        response.put("requestId", id);
        response.put("scriptId", script.getScriptId());
        response.put("oldMarks", oldMarks);
        response.put("newMarks", marks);
        response.put("status", "REVALUATION_COMPLETED");
        
        return ResponseEntity.ok(response);
    }

    
    // Get revaluation statistics for revaluator dashboard
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        List<RevaluationRequest> pending = revaluationRequestRepository.findByRevaluationStatus("REVALUATION_IN_PROGRESS");
        List<RevaluationRequest> completed = revaluationRequestRepository.findByRevaluationStatus("REVALUATION_COMPLETED");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingCount", pending.size());
        stats.put("completedCount", completed.size());
        stats.put("totalCount", pending.size() + completed.size());
        
        return ResponseEntity.ok(stats);
    }
}