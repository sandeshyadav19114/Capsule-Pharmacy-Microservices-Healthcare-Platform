package com.doctor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Doctor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;        // references User in user-service

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String specialization;

    private String qualification;  // e.g. MBBS, MD
    private String hospital;
    private String address;
    private Integer experienceYears;

    @Column(precision = 10, scale = 2)
    private BigDecimal consultationFee;

    private String availableDays;   // e.g. "MON,TUE,WED,THU,FRI"
    private String availableTimeFrom;  // e.g. "09:00"
    private String availableTimeTo;    // e.g. "17:00"

    private Double rating;
    private Integer totalReviews;

    @Builder.Default
    private Boolean available = true;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
