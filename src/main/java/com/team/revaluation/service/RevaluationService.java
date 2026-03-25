package com.team.revaluation.service;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.repository.AnswerScriptRepository;
import com.team.revaluation.repository.RevaluationRequestRepository;
import com.team.revaluation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RevaluationService {

    @Autowired
    private RevaluationRequestRepository revaluationRepo;
    @Autowired
    private AnswerScriptRepository scriptRepo;
    @Autowired
    private UserRepository userRepo;

    public RevaluationRequest applyForRevaluation(Long scriptId, Long studentId) {
        AnswerScript script = scriptRepo.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found"));
        Student student = (Student) userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        RevaluationRequest request = new RevaluationRequest();
        request.setAnswerScript(script);
        request.setStudent(student);
        request.setRevaluationStatus("PAYMENT_PENDING");
        
        return revaluationRepo.save(request);
    }
}