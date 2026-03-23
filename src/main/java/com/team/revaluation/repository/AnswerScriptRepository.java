package com.team.revaluation.repository;

import com.team.revaluation.model.AnswerScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnswerScriptRepository extends JpaRepository<AnswerScript, Long> {}