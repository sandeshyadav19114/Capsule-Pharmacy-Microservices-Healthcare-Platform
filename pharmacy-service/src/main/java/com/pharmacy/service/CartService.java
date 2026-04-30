package com.pharmacy.service;

import com.pharmacy.entity.*;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CartService — manages the patient's shopping cart.
 *
 * Rules:
 *  - One cart per patient (created on first add)
 *  - Prescription-required medicines need a verified prescriptionId
 *  - Stock is checked before adding
 *  - Total is recalculated on every mutation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;

    /**
     * Add a medicine to cart. Creates cart if patient doesn't have one.
     * If medicine already in cart, increments quantity.
     */
    public Cart addToCart(Long patientId, Long medicineId, int quantity, Long prescriptionId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + medicineId));

        if (!medicine.getActive()) {
            throw new IllegalStateException("Medicine is not available: " + medicine.getName());
        }
        if (medicine.getStockQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for: " + medicine.getName());
        }

        // Validate prescription for prescription-only medicines
        if (Boolean.TRUE.equals(medicine.getRequiresPrescription())) {
            if (prescriptionId == null) {
                throw new IllegalArgumentException(
                    medicine.getName() + " requires a valid prescription. Upload prescription first.");
            }
            Prescription prescription = prescriptionRepository.findByIdAndPatientId(prescriptionId, patientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Prescription not found or not yours"));
            if (!prescription.getVerified()) {
                throw new IllegalStateException("Prescription not yet verified by pharmacist.");
            }
        }

        Cart cart = cartRepository.findByPatientId(patientId)
                .orElseGet(() -> cartRepository.save(
                    Cart.builder().patientId(patientId).build()
                ));

        // If medicine already in cart, update quantity
        cart.getItems().stream()
                .filter(item -> item.getMedicine().getId().equals(medicineId))
                .findFirst()
                .ifPresentOrElse(
                    existingItem -> existingItem.setQuantity(existingItem.getQuantity() + quantity),
                    () -> cart.getItems().add(CartItem.builder()
                            .cart(cart)
                            .medicine(medicine)
                            .quantity(quantity)
                            .unitPrice(medicine.getPrice())
                            .prescriptionId(prescriptionId)
                            .build())
                );

        cart.recalculateTotal();
        log.info("Added {} x '{}' to cart for patientId={}", quantity, medicine.getName(), patientId);
        return cartRepository.save(cart);
    }

    public Cart removeFromCart(Long patientId, Long cartItemId) {
        Cart cart = getCartByPatientId(patientId);
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        cart.recalculateTotal();
        return cartRepository.save(cart);
    }

    public Cart updateQuantity(Long patientId, Long cartItemId, int newQuantity) {
        Cart cart = getCartByPatientId(patientId);
        cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .ifPresent(item -> {
                    if (item.getMedicine().getStockQuantity() < newQuantity) {
                        throw new IllegalStateException("Insufficient stock");
                    }
                    item.setQuantity(newQuantity);
                });
        cart.recalculateTotal();
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart getCartByPatientId(Long patientId) {
        return cartRepository.findByPatientId(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for patient: " + patientId));
    }

    public void clearCart(Long patientId) {
        cartRepository.findByPatientId(patientId).ifPresent(cart -> {
            cart.getItems().clear();
            cart.recalculateTotal();
            cartRepository.save(cart);
            log.info("Cart cleared for patientId={}", patientId);
        });
    }
}
