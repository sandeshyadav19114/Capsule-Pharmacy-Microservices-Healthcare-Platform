package com.pharmacy.kafka;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ── Consumed by Pharmacy Service from Payment Service ───────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class PaymentCompletedEvent {
    private Long orderId;
    private String orderNumber;
    private Long patientId;
    private String transactionId;
    private BigDecimal amount;
    private String paymentStatus; // SUCCESS | FAILED
    private LocalDateTime paymentTime;
}

// ── Published by Pharmacy when order is cancelled ────────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCancelledEvent {
    private Long orderId;
    private String orderNumber;
    private Long patientId;
    private String patientEmail;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
}

// ── Published by Pharmacy when order status changes ──────────────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class OrderStatusUpdatedEvent {
    private Long orderId;
    private String orderNumber;
    private Long patientId;
    private String patientEmail;
    private String newStatus;
    private LocalDateTime updatedAt;
}
