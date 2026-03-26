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

    // Student pages
    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "student/dashboard";
    }

    @GetMapping("/student/payment")
    public String payment() {
        return "student/payment";
    }

    @GetMapping("/student/revaluation-status")
    public String revaluationStatus() {
        return "student/revaluation-status";
    }

    // Dashboard redirect page
    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    // Admin pages
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/results")
    public String adminResults() {
        return "admin/results";
    }

    // Evaluator pages
    @GetMapping("/evaluator/dashboard")
    public String evaluatorDashboard() {
        return "evaluator/dashboard";
    }

    // Revaluator pages - Single mapping (removed duplicate)
    @GetMapping("/revaluator/dashboard")
    public String revaluatorDashboard() {
        return "revaluator/dashboard";
    }
}