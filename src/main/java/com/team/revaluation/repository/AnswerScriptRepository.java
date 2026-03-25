package com.team.revaluation.repository;

import com.team.revaluation.model.AnswerScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnswerScriptRepository extends JpaRepository<AnswerScript, Long> {
    List<AnswerScript> findByStudentUserId(Long studentId);
}