package com.notification.entity;

import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification Entity — persists every notification attempt.
 * Enables:
 *  - Audit trail (who was notified and when)
 *  - Retry logic (FAILED notifications can be retried)
 *  - Admin dashboard (show notification history)
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String recipientEmail;

    private String recipientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type; // ORDER_PLACED, ORDER_CONFIRMED, APPOINTMENT_REMINDER, etc.

    @Column(nullable = false)
    private String subject;

    @Column(length = 5000)
    private String body; // HTML content snapshot

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    private String errorMessage; // populated if status = FAILED

    private Long referenceId;    // orderId or appointmentId
    private String referenceType; // ORDER or APPOINTMENT

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime sentAt;

    private int retryCount;
}
