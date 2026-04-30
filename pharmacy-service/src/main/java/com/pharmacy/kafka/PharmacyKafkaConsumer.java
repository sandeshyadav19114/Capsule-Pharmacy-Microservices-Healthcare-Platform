package com.pharmacy.kafka;

import com.pharmacy.entity.Order;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * PharmacyKafkaConsumer — listens for events from other microservices.
 *
 * Listens on:
 *   payment-completed-topic → updates order status based on payment result
 *
 * Saga Compensating Transaction:
 *   - If payment SUCCESS → order moves to CONFIRMED → triggers PROCESSING
 *   - If payment FAILED  → order moves to CANCELLED (compensating action)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyKafkaConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
        topics = "${kafka.topics.payment-completed}",
        groupId = "pharmacy-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentCompleted(
            @Payload PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received PaymentCompletedEvent: orderId={} | status={} | partition={} | offset={}",
                event.getOrderId(), event.getPaymentStatus(), partition, offset);

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found for payment event: orderId={}", event.getOrderId());
                    return new RuntimeException("Order not found: " + event.getOrderId());
                });

        if ("SUCCESS".equalsIgnoreCase(event.getPaymentStatus())) {
            // Saga step: payment succeeded → confirm order, deduct stock
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentTransactionId(event.getTransactionId());
            order.setPaymentCompletedAt(event.getPaymentTime());
            order.setEstimatedDelivery(LocalDateTime.now().plusDays(3));
            log.info("Order {} CONFIRMED after successful payment: txn={}", order.getOrderNumber(), event.getTransactionId());

        } else {
            // Saga compensating transaction: payment failed → cancel order, restore stock
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancellationReason("Payment failed: " + event.getTransactionId());
            log.warn("Order {} CANCELLED due to payment failure.", order.getOrderNumber());
            // Stock restoration is handled in OrderService.cancelOrder()
        }

        orderRepository.save(order);
    }
}
