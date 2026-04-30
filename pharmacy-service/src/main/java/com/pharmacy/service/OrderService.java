package com.pharmacy.service;

import com.pharmacy.entity.*;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.kafka.OrderCancelledEvent;
import com.pharmacy.kafka.OrderPlacedEvent;
import com.pharmacy.kafka.OrderItemEvent;
import com.pharmacy.kafka.PharmacyKafkaProducer;
import com.pharmacy.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OrderService — manages the full order lifecycle.
 *
 * Saga Choreography Pattern:
 *
 *  [Patient places order]
 *       ↓ (Kafka: order-placed-topic)
 *  [Payment Service] → processes payment
 *       ↓ (Kafka: payment-completed-topic)
 *  [Pharmacy Service] → confirms order / cancels (compensating)
 *       ↓ (Kafka: order-confirmed-topic)
 *  [Notification Service] → sends confirmation email
 *
 * If payment fails → OrderStatus = CANCELLED → stock restored (compensating transaction)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final MedicineService medicineService;
    private final PharmacyKafkaProducer kafkaProducer;

    /**
     * Checkout: converts cart to order, publishes Kafka event.
     */
    public Order placeOrder(Long patientId, String deliveryAddress,
                            String city, String pincode, Long prescriptionId) {

        Cart cart = cartService.getCartByPatientId(patientId);

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty. Add medicines before placing order.");
        }

        // Build order items from cart
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> OrderItem.builder()
                        .medicineId(cartItem.getMedicine().getId())
                        .medicineName(cartItem.getMedicine().getName())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getUnitPrice())
                        .subtotal(cartItem.getUnitPrice()
                                .multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .patientId(patientId)
                .totalAmount(cart.getTotalAmount())
                .status(OrderStatus.PENDING)
                .deliveryAddress(deliveryAddress)
                .city(city)
                .pincode(pincode)
                .prescriptionId(prescriptionId)
                .build();

        // Set bidirectional relationship
        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created: {} for patientId={}", savedOrder.getOrderNumber(), patientId);

        // Clear cart after order placed
        cartService.clearCart(patientId);

        // Publish Kafka event → triggers Payment Service
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .patientId(patientId)
                .totalAmount(savedOrder.getTotalAmount())
                .deliveryAddress(deliveryAddress + ", " + city + " - " + pincode)
                .orderTime(LocalDateTime.now())
                .items(orderItems.stream()
                        .map(i -> new OrderItemEvent(
                                i.getMedicineId(), i.getMedicineName(),
                                i.getQuantity(), i.getUnitPrice()))
                        .collect(Collectors.toList()))
                .build();

        kafkaProducer.publishOrderPlaced(event);
        return savedOrder;
    }

    /**
     * Called by PharmacyKafkaConsumer after PaymentCompleted (SUCCESS).
     * Deducts medicine stock.
     */
    @Transactional
    public void confirmOrder(Long orderId, String transactionId) {
        Order order = getOrderById(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentTransactionId(transactionId);
        order.setPaymentCompletedAt(LocalDateTime.now());
        order.setEstimatedDelivery(LocalDateTime.now().plusDays(3));

        // Deduct stock for each item
        order.getItems().forEach(item ->
                medicineService.deductStock(item.getMedicineId(), item.getQuantity())
        );

        orderRepository.save(order);
        log.info("Order {} CONFIRMED | txn={}", order.getOrderNumber(), transactionId);
    }

    /**
     * Saga compensating transaction: cancel order and restore stock.
     */
    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel a delivered order.");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        orderRepository.save(order);

        // Restore stock if order was confirmed (stock was already deducted)
        if (previousStatus == OrderStatus.CONFIRMED || previousStatus == OrderStatus.PROCESSING) {
            order.getItems().forEach(item ->
                    medicineService.restoreStock(item.getMedicineId(), item.getQuantity())
            );
        }

        // Publish cancellation event → Notification Service sends email
        kafkaProducer.publishOrderCancelled(OrderCancelledEvent.builder()
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .patientId(order.getPatientId())
                .cancellationReason(reason)
                .cancelledAt(LocalDateTime.now())
                .build());

        log.info("Order {} CANCELLED | reason={}", order.getOrderNumber(), reason);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByPatient(Long patientId) {
        return orderRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public Order getOrderByPatientAndId(Long orderId, Long patientId) {
        return orderRepository.findByIdAndPatientId(orderId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDateTime.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}