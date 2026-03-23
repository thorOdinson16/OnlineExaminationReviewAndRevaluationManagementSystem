package com.team.revaluation.controller;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluator")
public class EvaluatorController {

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    // Get all scripts assigned for evaluation
    @GetMapping("/scripts")
    public ResponseEntity<List<AnswerScript>> getAllScripts() {
        return ResponseEntity.ok(answerScriptRepository.findAll());
    }

    // Submit marks for a script
    @PutMapping("/scripts/{scriptId}/submit")
    public ResponseEntity<AnswerScript> submitMarks(
            @PathVariable Long scriptId,
            @RequestParam Float marks) {
        AnswerScript script = answerScriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        script.setTotalMarks(marks);
        script.setStatus("EVALUATED");
        return ResponseEntity.ok(answerScriptRepository.save(script));
    }
}