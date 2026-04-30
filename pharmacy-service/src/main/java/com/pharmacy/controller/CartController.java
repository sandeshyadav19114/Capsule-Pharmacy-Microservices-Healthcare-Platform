package com.pharmacy.controller;

import com.pharmacy.entity.Cart;
import com.pharmacy.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CartController — REST API for patient cart management.
 *
 * All endpoints require PATIENT role.
 *
 *   GET    /api/cart             → view cart
 *   POST   /api/cart/add         → add medicine to cart
 *   PUT    /api/cart/{itemId}    → update quantity
 *   DELETE /api/cart/{itemId}    → remove item
 *   DELETE /api/cart/clear       → clear entire cart
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PATIENT')")
@Tag(name = "Cart", description = "Patient cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current patient's cart",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Cart> getCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCartByPatientId(extractPatientId(auth)));
    }

    @PostMapping("/add")
    @Operation(summary = "Add medicine to cart",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Cart> addToCart(
            @RequestParam Long medicineId,
            @RequestParam @Min(1) int quantity,
            @RequestParam(required = false) Long prescriptionId,
            Authentication auth) {

        return ResponseEntity.ok(
                cartService.addToCart(extractPatientId(auth), medicineId, quantity, prescriptionId));
    }

    @PutMapping("/{cartItemId}")
    @Operation(summary = "Update item quantity in cart",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Cart> updateQuantity(
            @PathVariable Long cartItemId,
            @RequestParam @Min(1) int quantity,
            Authentication auth) {

        return ResponseEntity.ok(
                cartService.updateQuantity(extractPatientId(auth), cartItemId, quantity));
    }

    @DeleteMapping("/{cartItemId}")
    @Operation(summary = "Remove item from cart",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Cart> removeItem(@PathVariable Long cartItemId, Authentication auth) {
        return ResponseEntity.ok(
                cartService.removeFromCart(extractPatientId(auth), cartItemId));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear all items from cart",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> clearCart(Authentication auth) {
        cartService.clearCart(extractPatientId(auth));
        return ResponseEntity.ok(Map.of("message", "Cart cleared successfully"));
    }

    private Long extractPatientId(Authentication auth) {
        Object details = ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) auth).getDetails();
        if (details instanceof Long) return (Long) details;
        return Long.parseLong(details.toString());
    }
}
