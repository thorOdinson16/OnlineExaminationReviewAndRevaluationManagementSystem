// File: src/main/java/com/team/revaluation/repository/NotificationRepository.java
package com.team.revaluation.repository;

import com.team.revaluation.model.Notification;
import com.team.revaluation.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStudent(Student student);
    List<Notification> findByStudentAndIsReadFalse(Student student);
}