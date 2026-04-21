package com.notification.kafka;

import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * NotificationKafkaConsumer — listens to events from all microservices
 * and triggers email notifications accordingly.
 *
 * Consumer Groups:
 *   notification-group — ensures each event is processed exactly once
 *                        by this service (not duplicated with other consumers)
 *
 * Error Handling:
 *   - Errors are caught and logged; notification is saved as FAILED
 *   - RetryScheduler picks up FAILED notifications for retry
 *   - This prevents one bad message from stopping the consumer thread
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;

    // ── Order Placed ──────────────────────────────────────────────────────────

    @KafkaListener(
        topics = "${kafka.topics.order-placed}",
        groupId = "notification-group"
    )
    public void handleOrderPlaced(
            @Payload OrderPlacedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderPlacedEvent: order={} | partition={} | offset={}",
                event.getOrderNumber(), partition, offset);
        try {
            notificationService.sendOrderPlacedNotification(
                    event.getPatientId(),
                    event.getPatientEmail(),
                    event.getPatientName(),
                    event.getOrderNumber(),
                    event.getTotalAmount(),
                    event.getDeliveryAddress(),
                    event.getOrderId()
            );
        } catch (Exception e) {
            log.error("Error processing OrderPlacedEvent for order {}: {}",
                    event.getOrderNumber(), e.getMessage());
        }
    }

    // ── Payment Completed ─────────────────────────────────────────────────────

    @KafkaListener(
        topics = "${kafka.topics.payment-completed}",
        groupId = "notification-group"
    )
    public void handlePaymentCompleted(
            @Payload PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received PaymentCompletedEvent: order={} | status={}",
                event.getOrderNumber(), event.getPaymentStatus());
        try {
            if ("SUCCESS".equalsIgnoreCase(event.getPaymentStatus())) {
                notificationService.sendPaymentConfirmedNotification(
                        event.getPatientId(),
                        event.getPatientEmail(),
                        event.getPatientName(),
                        event.getOrderNumber(),
                        event.getAmount(),
                        event.getTransactionId(),
                        event.getOrderId()
                );
            }
            // FAILED payment — order cancelled notification sent by OrderCancelledEvent
        } catch (Exception e) {
            log.error("Error processing PaymentCompletedEvent: {}", e.getMessage());
        }
    }

    // ── Order Cancelled ───────────────────────────────────────────────────────

    @KafkaListener(
        topics = "${kafka.topics.order-cancelled}",
        groupId = "notification-group"
    )
    public void handleOrderCancelled(
            @Payload OrderCancelledEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderCancelledEvent: order={}", event.getOrderNumber());
        try {
            notificationService.sendOrderCancelledNotification(
                    event.getPatientId(),
                    event.getPatientEmail(),
                    event.getPatientName() != null ? event.getPatientName() : "Patient",
                    event.getOrderNumber(),
                    event.getCancellationReason(),
                    event.getOrderId()
            );
        } catch (Exception e) {
            log.error("Error processing OrderCancelledEvent: {}", e.getMessage());
        }
    }

    // ── Appointment Booked ────────────────────────────────────────────────────

    @KafkaListener(
        topics = "${kafka.topics.appointment-booked}",
        groupId = "notification-group"
    )
    public void handleAppointmentBooked(
            @Payload AppointmentBookedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received AppointmentBookedEvent: appointmentId={}", event.getAppointmentId());
        try {
            notificationService.sendAppointmentBookedNotification(
                    event.getPatientId(),
                    event.getPatientEmail(),
                    event.getPatientName(),
                    event.getDoctorName(),
                    event.getSpecialization(),
                    event.getAppointmentTime(),
                    event.getAppointmentType(),
                    event.getAppointmentId()
            );
        } catch (Exception e) {
            log.error("Error processing AppointmentBookedEvent: {}", e.getMessage());
        }
    }

    // ── Appointment Reminder ──────────────────────────────────────────────────

    @KafkaListener(
        topics = "${kafka.topics.appointment-reminder}",
        groupId = "notification-group"
    )
    public void handleAppointmentReminder(
            @Payload AppointmentReminderEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received AppointmentReminderEvent: appointmentId={}", event.getAppointmentId());
        try {
            notificationService.sendAppointmentReminderNotification(
                    event.getPatientId(),
                    event.getPatientEmail(),
                    event.getPatientName(),
                    event.getDoctorName(),
                    event.getSpecialization(),
                    event.getAppointmentTime(),
                    event.getAppointmentType(),
                    event.getAppointmentId()
            );
        } catch (Exception e) {
            log.error("Error processing AppointmentReminderEvent: {}", e.getMessage());
        }
    }

    // ── Appointment Cancelled ─────────────────────────────────────────────────

    @KafkaListener(
        topics = "${kafka.topics.appointment-cancelled}",
        groupId = "notification-group"
    )
    public void handleAppointmentCancelled(
            @Payload AppointmentCancelledEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received AppointmentCancelledEvent: appointmentId={}", event.getAppointmentId());
        try {
            notificationService.sendAppointmentCancelledNotification(
                    event.getPatientId(),
                    event.getPatientEmail(),
                    event.getPatientName(),
                    event.getDoctorName(),
                    event.getAppointmentTime(),
                    event.getCancellationReason(),
                    event.getAppointmentId()
            );
        } catch (Exception e) {
            log.error("Error processing AppointmentCancelledEvent: {}", e.getMessage());
        }
    }
}
