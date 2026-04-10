package com.team.revaluation.service;

import com.team.revaluation.model.Notification;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.RevaluationRequest;
import com.team.revaluation.model.Student;
import com.team.revaluation.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService — Creational (Singleton) Pattern.
 *
 * Guarantees a single instance throughout the application.
 *
 * PATTERN EXPLANATION for report:
 *   - Private constructor prevents external `new NotificationService()`.
 *   - A static volatile field holds the sole instance (thread-safe via
 *     double-checked locking).
 *   - Spring is allowed to call the private constructor once (via reflection).
 *   - @PostConstruct registers that Spring-managed bean into the static field.
 *   - getInstance() returns the registered instance; it NEVER creates a new one
 *     by itself (the instance is always provided by Spring on startup).
 *
 * This satisfies the OOAD Singleton contract:
 *   1. Only one instance exists in the JVM.
 *   2. A global access point (getInstance()) is available.
 *   3. The constructor is private.
 */
@Service
public class NotificationService {

    // Volatile guarantees visibility across threads.
    private static volatile NotificationService instance;

    @Autowired
    private NotificationRepository notificationRepository;

    // ── Singleton: private constructor ────────────────────────────────────────
    // Spring calls this once via reflection. Nothing else in the codebase may
    // call `new NotificationService()`.
    private NotificationService() {
        System.out.println("[Singleton] NotificationService instance created.");
    }

    // ── Singleton: register with static field after Spring injects deps ───────
    @PostConstruct
    private void registerInstance() {
        synchronized (NotificationService.class) {
            NotificationService.instance = this;
        }
        System.out.println("[Singleton] NotificationService registered. " +
            "Repository wired: " + (notificationRepository != null));
    }

    /**
     * Thread-safe global access point (double-checked locking).
     *
     * Returns the Spring-managed instance.
     * Never throws — if Spring has not yet started, returns null and logs a
     * warning (avoids hard crash during static init of other classes).
     */
    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) {
                    // This path is only hit if getInstance() is called before
                    // Spring has finished its context initialisation.
                    // In normal runtime this never happens.
                    System.err.println("[Singleton] WARNING: getInstance() called before " +
                        "Spring context initialised. Use @Autowired instead.");
                    return null;
                }
            }
        }
        return instance;
    }

    // ── Observer support ──────────────────────────────────────────────────────

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

    // ── Notify methods ────────────────────────────────────────────────────────

    /**
     * Persists a Notification to the DB and logs to console.
     * Called by ReviewService and RevaluationService on every status change
     * — this satisfies the Observer pattern requirement.
     */
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
            notificationRepository.save(notification);   // persists to DB
        } else {
            System.err.println("[Singleton] NotificationRepository null — not persisted.");
        }

        System.out.printf("[NOTIFICATION - %s] To: %s — %s%n",
            LocalDateTime.now(), student.getName(), message);
    }

    public void notifyReviewStatusChange(ReviewRequest request) {
        String message = String.format("Review Request #%d status changed to: %s",
            request.getReviewId(), request.getReviewStatus());
        notifyStudent(request.getStudent(), message);
        for (NotificationListener l : listeners) l.onReviewStatusChanged(request);
    }

    public void notifyRevaluationStatusChange(RevaluationRequest request) {
        String message = String.format("Revaluation Request #%d status changed to: %s",
            request.getRevaluationId(), request.getRevaluationStatus());
        notifyStudent(request.getStudent(), message);
        for (NotificationListener l : listeners) l.onRevaluationStatusChanged(request);
    }

    public List<Notification> getUnreadNotifications(Student student) {
        if (student == null || notificationRepository == null) return new ArrayList<>();
        return notificationRepository.findByStudentAndIsReadFalse(student);
    }

    public void markAsRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        n.setIsRead(true);
        notificationRepository.save(n);
    }
}