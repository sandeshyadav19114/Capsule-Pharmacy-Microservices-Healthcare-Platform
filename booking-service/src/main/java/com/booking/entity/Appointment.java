package com.booking.entity;

import com.booking.enums.AppointmentStatus;
import com.booking.enums.AppointmentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String patientEmail;

    private String patientName;

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private String doctorName;

    private String specialization;

    @Column(nullable = false)
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AppointmentType appointmentType = AppointmentType.IN_PERSON;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    private BigDecimal consultationFee;
    private String paymentTransactionId;

    @Column(length = 1000)
    private String symptoms;

    @Column(length = 1000)
    private String doctorNotes;

    private String cancellationReason;

    @Builder.Default
    private Boolean reminderSent = false;  // set to true after reminder Kafka event published

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
