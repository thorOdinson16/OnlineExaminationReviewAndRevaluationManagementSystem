package com.team.revaluation.controller;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.service.RevaluationService;
import com.team.revaluation.service.ReviewService;
import com.team.revaluation.service.PaymentService;
import com.team.revaluation.model.Payment;
import com.team.revaluation.facade.ExamReviewFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private RevaluationService revaluationService;

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ExamReviewFacade examReviewFacade;

    // Apply for paper review - Using Facade
    @PostMapping("/review/apply")
    public ResponseEntity<Map<String, Object>> applyForReview(@RequestBody Map<String, Object> request) {
        try {
            Long studentId = ((Number) request.get("studentId")).longValue();
            Long scriptId = ((Number) ((Map) request.get("answerScript")).get("scriptId")).longValue();
            
            Map<String, Object> result = examReviewFacade.applyAndPay(studentId, scriptId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("message", "Failed to apply for review");
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get specific review by ID
    @GetMapping("/review/{reviewId}")
    public ResponseEntity<ReviewRequest> getReviewById(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReviewById(reviewId));
    }

    // View all review requests by student
    @GetMapping("/review/student/{studentId}")
    public ResponseEntity<List<ReviewRequest>> getMyReviews(@PathVariable Long studentId) {
        return ResponseEntity.ok(reviewService.getReviewsByStudent(studentId));
    }

    // Apply for full revaluation
    @PostMapping("/revaluation/apply")
    public ResponseEntity<RevaluationRequest> applyForRevaluation(
            @RequestParam Long scriptId, 
            @RequestParam Long studentId) {
        return ResponseEntity.ok(revaluationService.applyForRevaluation(scriptId, studentId));
    }

    // Get student exam results
    @GetMapping("/results/{studentId}")
    public ResponseEntity<List<AnswerScript>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(answerScriptRepository.findByStudentUserId(studentId));
    }

    // Pay for review request
    @PostMapping("/review/{reviewId}/pay")
    public ResponseEntity<ReviewRequest> payForReview(@PathVariable Long reviewId) {
        ReviewRequest updatedRequest = reviewService.processPaymentForReview(reviewId);
        return ResponseEntity.ok(updatedRequest);
    }
}