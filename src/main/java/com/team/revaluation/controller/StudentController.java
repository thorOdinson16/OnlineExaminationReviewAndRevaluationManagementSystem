package com.team.revaluation.controller;

import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private ReviewService reviewService;

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
}