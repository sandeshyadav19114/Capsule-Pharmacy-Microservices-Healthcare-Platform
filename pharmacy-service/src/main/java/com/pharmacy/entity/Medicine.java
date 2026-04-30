package com.pharmacy.entity;

import com.pharmacy.enums.MedicineCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Medicine Entity — represents available medicines in the pharmacy catalog.
 * Prescription-only medicines (requiresPrescription = true) are validated
 * against uploaded & OCR-verified prescriptions before allowing purchase.
 */
@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "Amoxicillin 500mg"

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicineCategory category;

    @Column(nullable = false)
    private Boolean requiresPrescription;

    @Column(length = 1000)
    private String description;

    private String dosageForm;       // Tablet, Capsule, Syrup, Injection
    private String strength;         // e.g., "500mg", "10mg/5ml"
    private String expiryDate;       // stored as string for simplicity

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
