package com.pharmacy.dto;

import com.pharmacy.enums.MedicineCategory;
import com.pharmacy.enums.OrderStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ─────────────────────────────────────────────
// MEDICINE DTOs
// ─────────────────────────────────────────────

class MedicineRequest {
    @NotBlank(message = "Medicine name is required")
    public String name;
    @NotBlank(message = "Manufacturer is required")
    public String manufacturer;
    @NotNull @DecimalMin("0.0") public BigDecimal price;
    @NotNull @Min(0) public Integer stockQuantity;
    @NotNull public MedicineCategory category;
    @NotNull public Boolean requiresPrescription;
    public String description;
    public String dosageForm;
    public String strength;
    public String expiryDate;
}

class MedicineResponse {
    public Long id;
    public String name;
    public String manufacturer;
    public BigDecimal price;
    public Integer stockQuantity;
    public MedicineCategory category;
    public Boolean requiresPrescription;
    public String description;
    public String dosageForm;
    public String strength;
    public Boolean active;
}

// ─────────────────────────────────────────────
// PRESCRIPTION DTOs
// ─────────────────────────────────────────────

class PrescriptionUploadResponse {
    public Long prescriptionId;
    public String s3ImageUrl;
    public List<String> extractedMedicines; // parsed from OpenAI response
    public String message;
    public LocalDateTime uploadedAt;
}

class PrescriptionVerifyRequest {
    @NotNull public Long prescriptionId;
    @NotNull public Boolean verified;
    public String notes;
}

// ─────────────────────────────────────────────
// CART DTOs
// ─────────────────────────────────────────────

class AddToCartRequest {
    @NotNull(message = "Medicine ID is required")
    public Long medicineId;
    @NotNull @Min(1) public Integer quantity;
    public Long prescriptionId; // required if medicine is prescription-only
}

class CartItemResponse {
    public Long cartItemId;
    public Long medicineId;
    public String medicineName;
    public String dosageForm;
    public Integer quantity;
    public BigDecimal unitPrice;
    public BigDecimal subtotal;
    public Boolean requiresPrescription;
    public Long prescriptionId;
}

class CartResponse {
    public Long cartId;
    public Long patientId;
    public List<CartItemResponse> items;
    public int totalItems;
    public BigDecimal totalAmount;
    public LocalDateTime updatedAt;
}

// ─────────────────────────────────────────────
// ORDER DTOs
// ─────────────────────────────────────────────

class PlaceOrderRequest {
    @NotBlank public String deliveryAddress;
    @NotBlank public String city;
    @NotBlank @Pattern(regexp = "\\d{6}", message = "Enter valid 6-digit pincode") public String pincode;
    public Long prescriptionId;
}

class OrderItemResponse {
    public Long medicineId;
    public String medicineName;
    public Integer quantity;
    public BigDecimal unitPrice;
    public BigDecimal subtotal;
}

class OrderResponse {
    public Long orderId;
    public String orderNumber;
    public Long patientId;
    public List<OrderItemResponse> items;
    public OrderStatus status;
    public BigDecimal totalAmount;
    public String deliveryAddress;
    public String city;
    public String pincode;
    public String paymentTransactionId;
    public LocalDateTime createdAt;
    public LocalDateTime estimatedDelivery;
}

// ─────────────────────────────────────────────
// KAFKA EVENT DTOs (shared with other services)
// ─────────────────────────────────────────────

class OrderPlacedEvent {
    public Long orderId;
    public String orderNumber;
    public Long patientId;
    public BigDecimal totalAmount;
    public String deliveryAddress;
    public LocalDateTime orderTime;
}

class PaymentCompletedEvent {
    public Long orderId;
    public String orderNumber;
    public String transactionId;
    public BigDecimal amount;
    public String status; // SUCCESS or FAILED
}

// ─────────────────────────────────────────────
// COMMON RESPONSE WRAPPER
// ─────────────────────────────────────────────

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
