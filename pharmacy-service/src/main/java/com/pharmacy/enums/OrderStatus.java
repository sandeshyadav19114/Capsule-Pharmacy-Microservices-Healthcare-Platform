package com.pharmacy.enums;

public enum OrderStatus {
    PENDING,        // Order created, awaiting payment
    CONFIRMED,      // Payment received
    PROCESSING,     // Medicines being packed
    SHIPPED,        // Out for delivery
    DELIVERED,      // Successfully delivered
    CANCELLED,      // Cancelled by patient or system
    REFUND_INITIATED
}
