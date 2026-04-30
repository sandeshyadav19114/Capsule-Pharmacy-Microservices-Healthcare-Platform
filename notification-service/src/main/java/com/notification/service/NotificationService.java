package com.notification.service;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import com.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NotificationService — orchestrates notification creation and dispatching.
 *
 * Pattern:
 *  1. Build Notification entity (with HTML body from EmailTemplateBuilder)
 *  2. Save to DB (status = PENDING)
 *  3. Dispatch via EmailService (async)
 *
 * This ensures we always have a DB record even if email delivery fails.
 * Failed notifications can be retried by the RetryScheduler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;

    // ── Order Notifications ───────────────────────────────────────────────────

    public void sendOrderPlacedNotification(Long patientId, String email, String name,
                                             String orderNumber, BigDecimal total,
                                             String address, Long orderId) {
        String body = templateBuilder.buildOrderPlacedEmail(name, orderNumber, total, address);
        Notification notification = createAndSave(
                patientId, email, name,
                NotificationType.ORDER_PLACED,
                "Order Placed — " + orderNumber,
                body, orderId, "ORDER"
        );
        emailService.sendHtmlEmail(notification);
    }

    public void sendPaymentConfirmedNotification(Long patientId, String email, String name,
                                                  String orderNumber, BigDecimal amount,
                                                  String transactionId, Long orderId) {
        String body = templateBuilder.buildPaymentConfirmedEmail(name, orderNumber, amount, transactionId);
        Notification notification = createAndSave(
                patientId, email, name,
                NotificationType.ORDER_CONFIRMED,
                "Payment Confirmed — Order " + orderNumber,
                body, orderId, "ORDER"
        );
        emailService.sendHtmlEmail(notification);
    }

    public void sendOrderCancelledNotification(Long patientId, String email, String name,
                                                String orderNumber, String reason, Long orderId) {
        String body = templateBuilder.buildOrderCancelledEmail(name, orderNumber, reason);
        Notification notification = createAndSave(
                patientId, email, name,
                NotificationType.ORDER_CANCELLED,
                "Order Cancelled — " + orderNumber,
                body, orderId, "ORDER"
        );
        emailService.sendHtmlEmail(notification);
    }

    // ── Appointment Notifications ─────────────────────────────────────────────

    public void sendAppointmentBookedNotification(Long patientId, String email, String name,
                                                   String doctorName, String specialization,
                                                   LocalDateTime apptTime, String apptType,
                                                   Long appointmentId) {
        String body = templateBuilder.buildAppointmentBookedEmail(
                name, doctorName, specialization, apptTime, apptType);
        Notification notification = createAndSave(
                patientId, email, name,
                NotificationType.APPOINTMENT_BOOKED,
                "Appointment Confirmed with Dr. " + doctorName,
                body, appointmentId, "APPOINTMENT"
        );
        emailService.sendHtmlEmail(notification);
    }

    public void sendAppointmentReminderNotification(Long patientId, String email, String name,
                                                     String doctorName, String specialization,
                                                     LocalDateTime apptTime, String apptType,
                                                     Long appointmentId) {
        String body = templateBuilder.buildAppointmentReminderEmail(
                name, doctorName, specialization, apptTime, apptType);
        Notification notification = createAndSave(
                patientId, email, name,
                NotificationType.APPOINTMENT_REMINDER,
                "Reminder: Appointment Tomorrow with Dr. " + doctorName,
                body, appointmentId, "APPOINTMENT"
        );
        emailService.sendHtmlEmail(notification);
    }

    public void sendAppointmentCancelledNotification(Long patientId, String email, String name,
                                                      String doctorName, LocalDateTime apptTime,
                                                      String reason, Long appointmentId) {
        String body = templateBuilder.buildAppointmentCancelledEmail(
                name, doctorName, apptTime, reason);
        Notification notification = createAndSave(
                patientId, email, name,
                NotificationType.APPOINTMENT_CANCELLED,
                "Appointment Cancelled",
                body, appointmentId, "APPOINTMENT"
        );
        emailService.sendHtmlEmail(notification);
    }

    // ── History & Retry ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getFailedNotifications() {
        return notificationRepository.findByStatus(NotificationStatus.FAILED);
    }

    public void retryFailedNotifications() {
        List<Notification> failed = notificationRepository
                .findByStatusAndRetryCountLessThan(NotificationStatus.FAILED, 3);

        log.info("Retrying {} failed notifications", failed.size());
        failed.forEach(notification -> {
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
            emailService.sendHtmlEmail(notification);
        });
    }

    // ── Private Helper ────────────────────────────────────────────────────────

    private Notification createAndSave(Long userId, String email, String name,
                                        NotificationType type, String subject,
                                        String body, Long refId, String refType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .recipientEmail(email)
                .recipientName(name)
                .type(type)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.PENDING)
                .referenceId(refId)
                .referenceType(refType)
                .retryCount(0)
                .build();
        return notificationRepository.save(notification);
    }


    @Transactional(readOnly = true)
    public long countSentToday() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        return notificationRepository.countSentToday(startOfDay);
    }
}