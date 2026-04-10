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
 * NotificationService — Creational (Singleton) Pattern.
 *
 * Guarantees a single instance throughout the application:
 *   - Private constructor prevents external instantiation via 'new'
 *   - Static volatile reference ensures visibility across threads
 *   - @PostConstruct registers the Spring-managed bean into the static field
 *   - getInstance() is thread-safe via double-checked locking
 *
 * Spring uses reflection to call the private constructor once.
 * All other code must obtain the instance via @Autowired or getInstance().
 */
@Service
public class NotificationService {

    // volatile guarantees that writes to 'instance' are visible across all threads immediately
    private static volatile NotificationService instance;

    @Autowired
    private NotificationRepository notificationRepository;

    // Private constructor — prevents 'new NotificationService()' anywhere in the codebase.
    // Spring bypasses this via reflection to create its managed bean.
    private NotificationService() {
        System.out.println("NotificationService (Singleton) initialised.");
    }

    /**
     * Thread-safe accessor using double-checked locking.
     * The first null check avoids synchronisation overhead on every call once initialised.
     * The second null check inside the synchronized block prevents a race condition where
     * two threads both pass the first check before either sets the instance.
     */
    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) {
                    throw new IllegalStateException(
                        "NotificationService.getInstance() called before Spring initialisation. " +
                        "Inject via @Autowired instead."
                    );
                }
            }
        }
        return instance;
    }

    // @PostConstruct registers the Spring-managed bean into the static field
    // so that getInstance() works for any non-Spring callers (tests, utilities)
    @jakarta.annotation.PostConstruct
    private void registerInstance() {
        synchronized (NotificationService.class) {
            instance = this;
        }
        System.out.println("NotificationService singleton registered. Repository injected: "
            + (notificationRepository != null));
    }

    // ==================== OBSERVER SUPPORT ====================

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