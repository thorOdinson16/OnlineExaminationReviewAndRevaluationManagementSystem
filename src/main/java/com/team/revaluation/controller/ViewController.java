package com.team.revaluation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    // ── Evaluator views ───────────────────────────────────────────────────────

    @GetMapping("/view/evaluator/dashboard")
    public String evaluatorDashboard() {
        return "evaluator/dashboard";
    }

    @GetMapping("/view/evaluator/scripts")
    public String evaluatorScripts() {
        return "evaluator/scripts";
    }

    // ── Revaluator views ──────────────────────────────────────────────────────

    @GetMapping("/view/revaluator/dashboard")
    public String revaluatorDashboard() {
        return "revaluator/dashboard";
    }

    @GetMapping("/view/revaluator/requests")
    public String revaluatorRequests() {
        return "revaluator/requests";
    }

    // ── Admin views ───────────────────────────────────────────────────────────

    @GetMapping("/view/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/view/admin/reviews")
    public String adminReviews() {
        return "admin/reviews";
    }

    @GetMapping("/view/admin/revaluations")
    public String adminRevaluations() {
        return "admin/revaluations";
    }

    @GetMapping("/view/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    // ── Student views ─────────────────────────────────────────────────────────

    @GetMapping("/student/reviewed-paper")
    public String studentReviewedPaper() {
        return "student/reviewed-paper";
    }

    // ← studentDashboard() REMOVED — already mapped in PageController
}