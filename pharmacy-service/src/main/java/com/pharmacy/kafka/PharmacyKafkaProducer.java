package com.pharmacy.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * PharmacyKafkaProducer — publishes order lifecycle events to Kafka topics.
 *
 * Topics produced:
 *   order-placed-topic    → consumed by Notification Service, Payment Service
 *   order-cancelled-topic → consumed by Notification Service, Payment Service
 *
 * Uses async send with callback logging for observability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-placed}")
    private String orderPlacedTopic;

    @Value("${kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    /**
     * Publish event when patient places an order.
     * Key = orderNumber ensures all events for same order go to same partition.
     */
    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.info("Publishing OrderPlacedEvent for order: {}", event.getOrderNumber());
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(orderPlacedTopic, event.getOrderNumber(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("OrderPlacedEvent sent successfully for order: {} | partition: {} | offset: {}",
                        event.getOrderNumber(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send OrderPlacedEvent for order: {} | error: {}",
                        event.getOrderNumber(), ex.getMessage());
            }
        });
    }

    /**
     * Publish event when an order is cancelled.
     * Consumed by Notification (send cancellation email) and Payment (trigger refund).
     */
    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Publishing OrderCancelledEvent for order: {}", event.getOrderNumber());
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(orderCancelledTopic, event.getOrderNumber(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("OrderCancelledEvent sent for order: {}", event.getOrderNumber());
            } else {
                log.error("Failed to send OrderCancelledEvent: {}", ex.getMessage());
            }
        });
    }
}
