package com.pharmacy.entity;

import com.pharmacy.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity — created when patient checks out from cart.
 * Status transitions: PENDING → CONFIRMED (after payment) → PROCESSING → SHIPPED → DELIVERED
 * Kafka event published on order creation and status change.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber; // e.g., ORD-2024-00001

    @Column(nullable = false)
    private Long patientId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Payment info (filled after Payment Service confirms)
    private String paymentTransactionId;
    private LocalDateTime paymentCompletedAt;

    // Delivery address (denormalized snapshot at order time)
    private String deliveryAddress;
    private String city;
    private String pincode;

    // Linked prescription (if any prescription-only medicines in order)
    private Long prescriptionId;

    private String cancellationReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime estimatedDelivery;
}
