package com.team.revaluation.service;

import com.team.revaluation.model.Notification;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService — Singleton pattern.
 *
 * Spring manages this as an application-scoped singleton bean.
 * The static instance reference lets non-Spring code (tests, utilities)
 * call getInstance() and get the same Spring-managed object.
 *
 * Constructor is private so that 'new NotificationService()' is impossible
 * outside this class — enforcing the singleton guarantee.
 * Spring uses reflection to instantiate it once via the @Service annotation.
 */
@Service
public class NotificationService {

    // 1. Private static instance — set after Spring initialises the bean
    private static NotificationService instance;

    @Autowired
    private NotificationRepository notificationRepository;

    // 2. Private constructor — no external code can call 'new NotificationService()'
    //    Spring uses reflection to bypass this restriction for its own injection.
    private NotificationService() {
        System.out.println("NotificationService (Singleton) initialised.");
    }

    // 3. Public static accessor — always returns the Spring-managed singleton
    public static NotificationService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "NotificationService.getInstance() called before Spring initialisation. " +
                "Inject NotificationService via @Autowired instead."
            );
        }
        return instance;
    }

    // 4. @PostConstruct registers the Spring-managed instance into the static field
    @jakarta.annotation.PostConstruct
    private void registerInstance() {
        instance = this;
        System.out.println("NotificationService singleton registered. Repository injected: "
            + (notificationRepository != null));
    }

    // ==================== OBSERVER SUPPORT ====================

    // List of registered listeners (Observer pattern)
    private final List<NotificationListener> listeners = new ArrayList<>();

    public interface NotificationListener {
        void onReviewStatusChanged(ReviewRequest request);
        void onRevaluationStatusChanged(RevaluationRequest request);
    }

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    // ==================== NOTIFY METHODS ====================

    public void notifyStudent(Student student, String message) {
        if (student == null) {
            System.out.printf("[NOTIFICATION - %s] System: %s%n", LocalDateTime.now(), message);
            return;
        }

        Notification notification = new Notification();
        notification.setStudent(student);
        notification.setMessage(message);
        notification.setIsRead(false);

        if (notificationRepository != null) {
            notificationRepository.save(notification);
        } else {
            System.err.println("NotificationRepository is null — notification not persisted.");
        }

        System.out.printf("[NOTIFICATION - %s] To: %s (%s) — %s%n",
            LocalDateTime.now(), student.getName(), student.getEmail(), message);

        sendEmailNotification(student, message);
        sendSmsNotification(student, message);
    }

    public void notifyReviewStatusChange(ReviewRequest request) {
        String message = String.format("Review Request #%d status changed to: %s",
            request.getReviewId(), request.getReviewStatus());

        notifyStudent(request.getStudent(), message);

        for (NotificationListener listener : listeners) {
            listener.onReviewStatusChanged(request);
        }
    }

    public void notifyRevaluationStatusChange(RevaluationRequest request) {
        String message = String.format("Revaluation Request #%d status changed to: %s",
            request.getRevaluationId(), request.getRevaluationStatus());

        notifyStudent(request.getStudent(), message);

        for (NotificationListener listener : listeners) {
            listener.onRevaluationStatusChanged(request);
        }
    }

    public List<Notification> getUnreadNotifications(Student student) {
        if (student == null || notificationRepository == null) return new ArrayList<>();
        return notificationRepository.findByStudentAndIsReadFalse(student);
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    // ==================== PRIVATE HELPERS ====================

    private void sendEmailNotification(Student student, String message) {
        System.out.printf("Email sent to %s: %s%n", student.getEmail(), message);
    }

    private void sendSmsNotification(Student student, String message) {
        System.out.printf("SMS sent to student %s: %s%n", student.getName(), message);
    }
}