package com.pharmacy.controller;

import com.pharmacy.entity.Order;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OrderController — REST API for order lifecycle management.
 *
 * Patient:
 *   POST /api/orders/place         → checkout cart → creates order → Kafka event
 *   GET  /api/orders/my            → patient's order history
 *   GET  /api/orders/{id}          → get single order
 *   PUT  /api/orders/{id}/cancel   → cancel order (Saga compensating tx)
 *
 * Admin:
 *   GET /api/orders/all            → all orders
 *   PUT /api/orders/{id}/status    → update order status (PROCESSING, SHIPPED etc.)
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement and lifecycle management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/place")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Place order from cart — triggers Kafka → Payment Service",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Order> placeOrder(
            @RequestParam @NotBlank String deliveryAddress,
            @RequestParam @NotBlank String city,
            @RequestParam @NotBlank String pincode,
            @RequestParam(required = false) Long prescriptionId,
            Authentication auth) {

        Long patientId = extractPatientId(auth);
        Order order = orderService.placeOrder(patientId, deliveryAddress, city, pincode, prescriptionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get all orders for current patient",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Order>> getMyOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getOrdersByPatient(extractPatientId(auth)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    @Operation(summary = "Get order by ID",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Order> getOrder(@PathVariable Long id, Authentication auth) {
        // Patients can only see their own orders; admins can see all
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return ResponseEntity.ok(orderService.getOrderById(id));
        }
        return ResponseEntity.ok(orderService.getOrderByPatientAndId(id, extractPatientId(auth)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    @Operation(summary = "Cancel order — triggers Saga compensating transaction",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> cancelOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Cancelled by customer") String reason,
            Authentication auth) {

        orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(Map.of(
                "message", "Order cancelled successfully",
                "orderId", id.toString()
        ));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (Admin only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status — PROCESSING, SHIPPED, DELIVERED (Admin only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }

    private Long extractPatientId(Authentication auth) {
        Object details = ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) auth).getDetails();
        if (details instanceof Long) return (Long) details;
        return Long.parseLong(details.toString());
    }
}
