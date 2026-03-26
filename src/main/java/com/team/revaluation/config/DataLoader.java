package com.team.revaluation.config;

import com.team.revaluation.factory.UserFactory;
import com.team.revaluation.model.*;
import com.team.revaluation.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

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
        // Create sample data only if database is empty
        if (userRepository.count() == 0) {
            System.out.println("📊 Loading sample data...");
            
            // Create sample users
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
            
            // Create sample exams
            Exam exam1 = new Exam();
            exam1.setSubject("Data Structures");
            exam1.setExamDate(LocalDate.of(2024, 1, 15));
            
            Exam exam2 = new Exam();
            exam2.setSubject("Algorithms");
            exam2.setExamDate(LocalDate.of(2024, 1, 20));
            
            examRepository.save(exam1);
            examRepository.save(exam2);
            
            // Create sample answer scripts
            AnswerScript script1 = new AnswerScript();
            script1.setStudent(student1);
            script1.setExam(exam1);
            script1.setStatus("EVALUATED");
            script1.setTotalMarks(75.5f);
            
            AnswerScript script2 = new AnswerScript();
            script2.setStudent(student1);
            script2.setExam(exam2);
            script2.setStatus("SUBMITTED");
            script2.setTotalMarks(null);
            
            AnswerScript script3 = new AnswerScript();
            script3.setStudent(student2);
            script3.setExam(exam1);
            script3.setStatus("RESULTS_PUBLISHED");
            script3.setTotalMarks(85.0f);
            
            answerScriptRepository.save(script1);
            answerScriptRepository.save(script2);
            answerScriptRepository.save(script3);
            
            System.out.println("✅ Sample data loaded successfully!");
            System.out.println("   - Students: John Doe, Jane Smith");
            System.out.println("   - Evaluator: Dr. Robert Johnson");
            System.out.println("   - Revaluator: Dr. Emily Davis");
            System.out.println("   - Admin: Admin User (admin@example.com / admin123)");
        }
    }
}