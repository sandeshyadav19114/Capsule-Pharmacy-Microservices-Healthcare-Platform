package com.pharmacy.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; /**
 * Kafka Event DTOs — these represent the messages published/consumed
 * on Kafka topics across the Capsule Pharmacy microservices.
 *
 * OrderPlacedEvent:     published by Pharmacy → consumed by Notification, Payment
 * PaymentCompletedEvent: published by Payment  → consumed by Pharmacy, Notification
 * OrderCancelledEvent:  published by Pharmacy → consumed by Notification, Payment
 */

// ── Published by Pharmacy Service when patient places order ──────────────────
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPlacedEvent {
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
