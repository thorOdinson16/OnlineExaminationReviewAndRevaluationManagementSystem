package com.team.revaluation.service;

import com.team.revaluation.factory.PaymentProcessorFactory;
import com.team.revaluation.model.Notification;
import com.team.revaluation.model.Payment;
import com.team.revaluation.model.Student;
import com.team.revaluation.repository.NotificationRepository;
import com.team.revaluation.repository.PaymentRepository;
import com.team.revaluation.repository.UserRepository;
import com.team.revaluation.repository.AnswerScriptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * PaymentService — orchestrates payment processing.
 *
 * Patterns in use (all wired, not dead code):
 *   Abstract Factory : PaymentProcessorFactory (Spring bean, @Autowired)
 *   Proxy            : PaymentProxy (validates amount > 0 and student exists)
 *   Decorator        : PaymentLoggingDecorator (wraps proxy for audit logging)
 *   Chain of Resp.   : 4-handler chain (amount → student → script → gateway)
 *
 * FIX: paymentProxy.setStudent() is now called BEFORE the validation chain runs,
 * so the Proxy has student context at the point it needs it. Previously it was
 * called AFTER the chain, making the proxy guard check redundant/untestable.
 *
 * Chain order:  amount → student exists → script status → gateway
 * Gateway chain: Proxy (guards) → Decorator (logs) → Singleton gateway
 */
@Service
public class PaymentService {

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AnswerScriptRepository answerScriptRepository;

    @Autowired private AmountValidationHandler       amountValidationHandler;
    @Autowired private StudentExistsValidationHandler studentExistsValidationHandler;
    @Autowired private ScriptStatusValidationHandler  scriptStatusValidationHandler;
    @Autowired private GatewayValidationHandler       gatewayValidationHandler;

    @Autowired private PaymentProcessorFactory paymentProcessorFactory;

    private IPaymentGateway paymentGateway;
    private PaymentProxy    paymentProxy;
    private PaymentValidationHandler validationChain;

    @PostConstruct
    private void init() {
        // Proxy (validates amount > 0 and student exists before gateway)
        IPaymentGateway realGateway = PaymentGatewaySingleton.getInstance();
        this.paymentProxy = new PaymentProxy(realGateway);

        // Decorator (wraps proxy with audit logging)
        this.paymentGateway = new PaymentLoggingDecorator(paymentProxy);

        // Chain of Responsibility
        amountValidationHandler.setNext(studentExistsValidationHandler);
        studentExistsValidationHandler.setNext(scriptStatusValidationHandler);
        scriptStatusValidationHandler.setNext(gatewayValidationHandler);
        this.validationChain = amountValidationHandler;

        System.out.println("[PaymentService] Initialized: CoR \u2192 Proxy \u2192 Decorator \u2192 Gateway");
    }

    @Transactional
    public Payment processPayment(Payment payment) {

        // FIX: Set student on Proxy BEFORE the chain runs, so the Proxy's
        // guard check has the student context it needs.
        if (payment.getStudent() != null) {
            paymentProxy.setStudent(payment.getStudent());
        }

        // 1. Chain of Responsibility: amount → student exists → script status → gateway
        try {
            validationChain.handle(payment, userRepository);
        } catch (RuntimeException e) {
            payment.setPaymentStatus("FAILED");
            System.err.println("[PaymentService] Validation failed: " + e.getMessage());
            return paymentRepository.save(payment);
        }

        // 2. Abstract Factory: obtain processor through the factory BEAN
        PaymentProcessor processor = paymentProcessorFactory.getPaymentProcessor(payment.getPaymentType());
        boolean processorOk = processor.process(payment);

        // 3. Proxy + Decorator: process through gateway chain
        boolean gatewayOk = paymentGateway.processTransaction(payment.getAmount());

        payment.setPaymentStatus((processorOk && gatewayOk) ? "SUCCESS" : "FAILED");
        return paymentRepository.save(payment);
    }

    // ── Read helpers ──────────────────────────────────────────────────────────

    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    public List<Payment> getPaymentsByStudent(Long studentId) {
        Student student = (Student) userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        return paymentRepository.findByStudent(student);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByPaymentStatus(status);
    }

    public List<Notification> getUnreadNotifications(Long studentId) {
        Student student = (Student) userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        return notificationRepository.findByStudentAndIsReadFalse(student);
    }

    public void markNotificationAsRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    public List<Notification> getNotificationsByStudent(Long studentId) {
        Student student = (Student) userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        return notificationRepository.findByStudent(student);
    }
}