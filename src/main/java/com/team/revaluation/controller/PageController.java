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

    @GetMapping("/revaluator/dashboard")
    public String revaluatorDashboard() {
        return "revaluator/dashboard";
    }

    @GetMapping("/admin/results")
    public String adminResults() {
        return "admin/results";
    }
}