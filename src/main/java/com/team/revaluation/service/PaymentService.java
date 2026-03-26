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
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AnswerScriptRepository answerScriptRepository;
    
    @Autowired
    private ScriptStatusValidationHandler scriptStatusValidationHandler;
    
    @Autowired
    private AmountValidationHandler amountValidationHandler;
    
    @Autowired
    private StudentExistsValidationHandler studentExistsValidationHandler;
    
    @Autowired
    private GatewayValidationHandler gatewayValidationHandler;
    
    private IPaymentGateway paymentGateway;
    private PaymentValidationHandler validationChain;
    
    public PaymentService() {
        // ✅ Create real gateway
        IPaymentGateway realGateway = PaymentGatewaySingleton.getInstance();
        
        // ✅ Wrap with Proxy for input validation (Proxy pattern)
        IPaymentGateway proxyGateway = new PaymentProxy(realGateway);
        
        // ✅ Wrap with Decorator for logging (Decorator pattern)
        this.paymentGateway = new PaymentLoggingDecorator(proxyGateway);
        
        System.out.println("PaymentService initialized with Proxy → Decorator → Gateway");
    }
    
    private void initializeValidationChain() {
        // Build Chain of Responsibility
        amountValidationHandler.setNext(studentExistsValidationHandler);
        studentExistsValidationHandler.setNext(scriptStatusValidationHandler);
        scriptStatusValidationHandler.setNext(gatewayValidationHandler);
        this.validationChain = amountValidationHandler;
    }

    @Transactional
    public Payment processPayment(Payment payment) {
        // Initialize validation chain if not already done
        if (validationChain == null) {
            initializeValidationChain();
        }
        
        // Run through validation chain
        try {
            validationChain.handle(payment, userRepository);
        } catch (RuntimeException e) {
            payment.setPaymentStatus("FAILED");
            System.err.println("Payment validation failed: " + e.getMessage());
            return paymentRepository.save(payment);
        }
        
        // Get appropriate payment processor using Abstract Factory
        PaymentProcessor processor = PaymentProcessorFactory.getPaymentProcessor(payment.getPaymentType());
        boolean processorSuccess = processor.process(payment);
        
        // ✅ Process through Proxy + Decorator chain
        boolean gatewaySuccess = paymentGateway.processTransaction(payment.getAmount());
        
        // Update the database status based on the result
        if (processorSuccess && gatewaySuccess) {
            payment.setPaymentStatus("SUCCESS");
        } else {
            payment.setPaymentStatus("FAILED");
        }
        
        return paymentRepository.save(payment);
    }
    
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));
    }
    
    public List<Payment> getPaymentsByStudent(Long studentId) {
        Student student = (Student) userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
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
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return notificationRepository.findByStudentAndIsReadFalse(student);
    }
    
    public void markNotificationAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    public List<Notification> getNotificationsByStudent(Long studentId) {
        Student student = (Student) userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return notificationRepository.findByStudent(student);
    }
}