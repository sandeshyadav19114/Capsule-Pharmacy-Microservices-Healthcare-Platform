package com.notification.kafka;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka Event DTOs consumed by Notification Service.
 * Mirror of the event classes produced by other services.
 * Must match the exact JSON structure produced by publishers.
 */

// ── From Pharmacy Service ────────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class OrderPlacedEvent {
    private Long orderId;
    private String orderNumber;
    private Long patientId;
    private String patientEmail;
    private String patientName;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private List<OrderItemEvent> items;
    private LocalDateTime orderTime;
}

@Data @NoArgsConstructor @AllArgsConstructor
class OrderItemEvent {
    private Long medicineId;
    private String medicineName;
    private Integer quantity;
    private BigDecimal unitPrice;
}

@Data @NoArgsConstructor @AllArgsConstructor @Builder
class OrderCancelledEvent {
    private Long orderId;
    private String orderNumber;
    private Long patientId;
    private String patientName;
    private String patientEmail;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
}

// ── From Payment Service ─────────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class PaymentCompletedEvent {
    private Long orderId;
    private String orderNumber;
    private Long patientId;
    private String patientEmail;
    private String patientName;
    private String transactionId;
    private BigDecimal amount;
    private String paymentStatus;  // SUCCESS | FAILED
    private LocalDateTime paymentTime;
}

// ── From Booking Service ─────────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class AppointmentBookedEvent {
    private Long appointmentId;
    private Long patientId;
    private String patientEmail;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private LocalDateTime appointmentTime;
    private String appointmentType;   // ONLINE or IN_PERSON
    private String notes;
}

@Data @NoArgsConstructor @AllArgsConstructor @Builder
class AppointmentCancelledEvent {
    private Long appointmentId;
    private Long patientId;
    private String patientEmail;
    private String patientName;
    private String doctorName;
    private LocalDateTime appointmentTime;
    private String cancellationReason;
}

@Data @NoArgsConstructor @AllArgsConstructor @Builder
class AppointmentReminderEvent {
    private Long appointmentId;
    private Long patientId;
    private String patientEmail;
    private String patientName;
    private String doctorName;
    private String specialization;
    private LocalDateTime appointmentTime;
    private String appointmentType;
}
