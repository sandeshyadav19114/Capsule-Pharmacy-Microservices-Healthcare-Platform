package com.payment.controller;

import com.payment.entity.Payment;
import com.payment.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * PaymentController — payment history and status queries.
 *
 * GET /api/payments/my           → patient's payment history (PATIENT)
 * GET /api/payments/{txn}        → get by transaction ID (PATIENT/ADMIN)
 * GET /api/payments/order/{id}   → get payment by order ID (PATIENT/ADMIN)
 * GET /api/payments/all          → all payments (ADMIN)
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment history and status")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get payment history for current patient",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Payment>> getMyPayments(Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(paymentRepository.findByPatientIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    @Operation(summary = "Get payment by transaction ID",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Payment> getByTransactionId(@PathVariable String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    @Operation(summary = "Get payment by order ID",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Payment> getByOrderId(@PathVariable Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments (Admin only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentRepository.findAll());
    }

    private Long extractUserId(Authentication auth) {
        Object d = ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) auth).getDetails();
        return d instanceof Long ? (Long) d : Long.parseLong(d.toString());
    }
}
