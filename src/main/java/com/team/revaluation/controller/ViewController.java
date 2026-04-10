package com.team.revaluation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * FIX: Added /student/reviewed-paper route to serve the new Thymeleaf page.
 * This satisfies the missing "view reviewed paper" checklist item (§3.3 Abyud Shetty).
 *
 * Full updated ViewController — replace the existing file with this.
 */
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

    /**
     * FIX: New route for the reviewed-paper page.
     *
     * Satisfies checklist §3.3 (Abyud Shetty):
     *   "Thymeleaf pages: view reviewed paper, apply revaluation, payment, status"
     *
     * The page is at: src/main/resources/templates/student/reviewed-paper.html
     * URL: /student/reviewed-paper?reviewId={id}&scriptId={id}
     *
     * Linked from student dashboard when review status is VERIFIED or
     * AWAIT_STUDENT_DECISION — student clicks "View Reviewed Paper" to
     * reach this page, where they can accept marks or escalate to revaluation.
     */
    @GetMapping("/student/reviewed-paper")
    public String studentReviewedPaper() {
        return "student/reviewed-paper";
    }

    /**
     * Route for student dashboard (convenience — avoids hardcoding in links).
     */
    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "student/dashboard";
    }
}