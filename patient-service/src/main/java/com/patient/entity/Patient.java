package com.patient.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Patient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;

    @Column(length = 1000)
    private String address;
    private String city;
    private String pincode;

    // Medical history
    @Column(length = 2000)
    private String allergies;

    @Column(length = 2000)
    private String chronicConditions;

    private String emergencyContactName;
    private String emergencyContactPhone;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
