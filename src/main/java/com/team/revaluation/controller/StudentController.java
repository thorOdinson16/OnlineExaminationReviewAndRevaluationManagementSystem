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

import java.util.List;

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

    // Apply for paper review
    @PostMapping("/review/apply")
    public ResponseEntity<ReviewRequest> applyForReview(@RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.applyForReview(request));
    }

    // View all review requests by student
    @GetMapping("/review/{studentId}")
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

    // NEW: Get student exam results
    @GetMapping("/results/{studentId}")
    public ResponseEntity<List<AnswerScript>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(answerScriptRepository.findByStudentUserId(studentId));
    }

    // NEW: Pay for review request
    @PostMapping("/review/{reviewId}/pay")
    public ResponseEntity<ReviewRequest> payForReview(@PathVariable Long reviewId) {
        ReviewRequest updatedRequest = reviewService.processPaymentForReview(reviewId);
        return ResponseEntity.ok(updatedRequest);
    }
}