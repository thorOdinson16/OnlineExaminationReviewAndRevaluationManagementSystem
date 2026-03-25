// File: src/main/java/com/team/revaluation/controller/PageController.java
package com.team.revaluation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    // Home page redirects to login
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    // Authentication pages
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

    @GetMapping("/student/notifications")
    public String notifications() {
        return "student/notifications";
    }

    // Evaluator pages
    @GetMapping("/evaluator/dashboard")
    public String evaluatorDashboard() {
        return "evaluator/dashboard";
    }

    @GetMapping("/evaluator/scripts")
    public String evaluatorScripts() {
        return "evaluator/scripts";
    }

    // Revaluator pages
    @GetMapping("/revaluator/dashboard")
    public String revaluatorDashboard() {
        return "revaluator/dashboard";
    }

    @GetMapping("/revaluator/requests")
    public String revaluatorRequests() {
        return "revaluator/requests";
    }

    // Admin pages
    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/reviews")
    public String adminReviews() {
        return "admin/reviews";
    }

    @GetMapping("/admin/revaluations")
    public String adminRevaluations() {
        return "admin/revaluations";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }
}