package com.team.revaluation.controller;

import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.repository.RevaluationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/revaluator")
public class RevaluatorController {

    @Autowired
    private RevaluationRequestRepository revaluationRequestRepository;

    @GetMapping("/requests")
    public ResponseEntity<List<RevaluationRequest>> getAllRequests() {
        return ResponseEntity.ok(revaluationRequestRepository.findAll());
    }

    @PutMapping("/requests/{id}/submit")
    public ResponseEntity<RevaluationRequest> submitRevaluationMarks(
            @PathVariable Long id,
            @RequestParam Float marks) {
        RevaluationRequest req = revaluationRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setRevaluationStatus("COMPLETED");
        req.getAnswerScript().setTotalMarks(marks);
        return ResponseEntity.ok(revaluationRequestRepository.save(req));
    }
}