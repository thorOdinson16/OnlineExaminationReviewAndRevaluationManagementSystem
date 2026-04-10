package com.team.revaluation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    // ==================== STUDENT PAGES ====================

    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "student/dashboard";
    }

    // Dedicated apply-for-review page (Abhijna checklist item)
    @GetMapping("/student/apply-for-review")
    public String applyForReview() {
        return "student/apply-for-review";
    }

    // Payment confirm page (Abhijna checklist item)
    @GetMapping("/student/payment")
    public String payment() {
        return "student/payment";
    }

    // Payment confirm alias so it can also be reached as /student/payment-confirm
    @GetMapping("/student/payment-confirm")
    public String paymentConfirm() {
        return "student/payment";
    }

    // Status tracker page (Abhijna checklist item)
    @GetMapping("/student/status-tracker")
    public String statusTracker() {
        return "student/status-tracker";
    }

    @GetMapping("/student/revaluation-status")
    public String revaluationStatus() {
        return "student/revaluation-status";
    }

    // ==================== DASHBOARD REDIRECT ====================

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    // ==================== ADMIN PAGES ====================

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/results")
    public String adminResults() {
        return "admin/results";
    }

    // ==================== EVALUATOR PAGES ====================

    @GetMapping("/evaluator/dashboard")
    public String evaluatorDashboard() {
        return "evaluator/dashboard";
    }

    // ==================== REVALUATOR PAGES ====================

    @GetMapping("/revaluator/dashboard")
    public String revaluatorDashboard() {
        return "revaluator/dashboard";
    }
}