package com.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Prescription Entity — stores metadata after a patient uploads a prescription image.
 * The actual image is stored in AWS S3.
 * OpenAI GPT Vision extracts medicine names, stored in extractedMedicines.
 * Verified flag is set true once a pharmacist/admin reviews it.
 */
@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId; // references Patient microservice

    @Column(nullable = false)
    private Long doctorId;  // references Doctor microservice (can be 0 if self-uploaded)

    @Column(nullable = false)
    private String s3ImageKey; // S3 object key for the prescription image

    @Column(nullable = false)
    private String s3ImageUrl; // pre-signed or public URL

    // Extracted by OpenAI Vision API — comma-separated medicine names
    @Column(length = 2000)
    private String extractedMedicines;

    // Raw OpenAI response for audit
    @Column(length = 5000)
    private String openAiRawResponse;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false; // set to true by pharmacist review

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime verifiedAt;
    private String verifiedBy; // username of admin/pharmacist who verified
}
