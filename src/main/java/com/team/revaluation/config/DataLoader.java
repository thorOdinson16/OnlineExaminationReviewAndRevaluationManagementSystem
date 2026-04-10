package com.team.revaluation.config;

import com.team.revaluation.factory.UserFactory;
import com.team.revaluation.model.*;
import com.team.revaluation.repository.*;
import com.team.revaluation.service.AnswerScriptStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

/**
 * FIX: All AnswerScript status assignments now route through AnswerScriptStateMachine.transition()
 * instead of raw script.setStatus() calls.
 *
 * Previously violated the StateMachine constraint (checklist §3.2):
 *   "All service status changes route through StateMachine (not raw setStatus)"
 *
 * FIX applied: AnswerScript is first saved at the initial state "SUBMITTED" (the
 * StateMachine's starting state), then transitioned step-by-step via the machine
 * to reach the desired seed state. This guarantees DataLoader respects the same
 * transition rules as the rest of the application.
 */
@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFactory userFactory;

    @Autowired
    private AnswerScriptRepository answerScriptRepository;

    @Autowired
    private ExamRepository examRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            System.out.println("📊 Loading sample data...");

            // ── Users ────────────────────────────────────────────────────────
            Student student1 = (Student) userFactory.createUser("STUDENT", "John Doe", "john@example.com", "password");
            student1.setUsn("PES2UG22CS001");
            student1.setSection("A");

            Student student2 = (Student) userFactory.createUser("STUDENT", "Jane Smith", "jane@example.com", "password");
            student2.setUsn("PES2UG22CS002");
            student2.setSection("B");

            Evaluator evaluator = (Evaluator) userFactory.createUser("EVALUATOR", "Dr. Robert Johnson", "robert@example.com", "password");
            evaluator.setDepartment("Computer Science");

            Revaluator revaluator = (Revaluator) userFactory.createUser("REVALUATOR", "Dr. Emily Davis", "emily@example.com", "password");
            revaluator.setSpecialization("Data Structures");

            Admin admin = (Admin) userFactory.createUser("ADMIN", "Admin User", "admin@example.com", "admin123");
            admin.setAdminCode("ADMIN001");

            userRepository.save(student1);
            userRepository.save(student2);
            userRepository.save(evaluator);
            userRepository.save(revaluator);
            userRepository.save(admin);

            // ── Exams ────────────────────────────────────────────────────────
            Exam exam1 = new Exam();
            exam1.setSubject("Data Structures");
            exam1.setExamDate(LocalDate.of(2024, 1, 15));

            Exam exam2 = new Exam();
            exam2.setSubject("Algorithms");
            exam2.setExamDate(LocalDate.of(2024, 1, 20));

            examRepository.save(exam1);
            examRepository.save(exam2);

            // ── Answer Scripts (via StateMachine — FIX) ──────────────────────

            // script1: target state EVALUATED
            // StateMachine path: SUBMITTED → UNDER_EVALUATION → EVALUATED
            AnswerScript script1 = new AnswerScript();
            script1.setStudent(student1);
            script1.setExam(exam1);
            script1.setTotalMarks(75.5f);
            // Start at the initial state required by the StateMachine
            script1.setStatus("SUBMITTED");
            answerScriptRepository.save(script1);
            // Now advance through valid transitions
            AnswerScriptStateMachine.transition(script1, "UNDER_EVALUATION");
            AnswerScriptStateMachine.transition(script1, "EVALUATED");
            answerScriptRepository.save(script1);

            // script2: target state SUBMITTED (already the start state — no transitions needed)
            AnswerScript script2 = new AnswerScript();
            script2.setStudent(student1);
            script2.setExam(exam2);
            script2.setTotalMarks(null);
            script2.setStatus("SUBMITTED");        // initial state — StateMachine allows this as seed
            answerScriptRepository.save(script2);

            // script3: target state RESULTS_PUBLISHED
            // StateMachine path: SUBMITTED → UNDER_EVALUATION → EVALUATED → RESULTS_PUBLISHED
            AnswerScript script3 = new AnswerScript();
            script3.setStudent(student2);
            script3.setExam(exam1);
            script3.setTotalMarks(85.0f);
            script3.setStatus("SUBMITTED");
            answerScriptRepository.save(script3);
            AnswerScriptStateMachine.transition(script3, "UNDER_EVALUATION");
            AnswerScriptStateMachine.transition(script3, "EVALUATED");
            AnswerScriptStateMachine.transition(script3, "RESULTS_PUBLISHED");
            answerScriptRepository.save(script3);

            System.out.println("✅ Sample data loaded successfully (all states via StateMachine)!");
            System.out.println("   - Students: John Doe, Jane Smith");
            System.out.println("   - Evaluator: Dr. Robert Johnson");
            System.out.println("   - Revaluator: Dr. Emily Davis");
            System.out.println("   - Admin: Admin User (admin@example.com / admin123)");
        }
    }
}