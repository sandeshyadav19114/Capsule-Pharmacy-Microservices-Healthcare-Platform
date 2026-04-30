package com.payment.kafka;

import com.payment.entity.Payment;
import com.payment.enums.PaymentMethod;
import com.payment.enums.PaymentStatus;
import com.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PaymentKafkaConsumer — listens for order-placed events and processes payment.
 *
 * Saga step:
 *  1. Receive OrderPlacedEvent from Pharmacy Service
 *  2. Process payment (simulate success; in prod calls Razorpay/Stripe)
 *  3. Publish PaymentCompletedEvent (SUCCESS or FAILED)
 *  4. Pharmacy Service receives result → confirms or cancels order
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaConsumer {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-completed}")
    private String paymentCompletedTopic;

    @KafkaListener(topics = "${kafka.topics.order-placed}", groupId = "payment-group")
    @Transactional
    public void handleOrderPlaced(@Payload Map<String, Object> event,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received OrderPlacedEvent: order={} | partition={} | offset={}",
                event.get("orderNumber"), partition, offset);

        Long orderId = Long.parseLong(event.get("orderId").toString());
        String orderNumber = (String) event.get("orderNumber");
        Long patientId = Long.parseLong(event.get("patientId").toString());
        String patientEmail = (String) event.getOrDefault("patientEmail", "");
        BigDecimal amount = new BigDecimal(event.get("totalAmount").toString());

        // Check for duplicate (idempotent consumer)
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Duplicate OrderPlacedEvent for orderId={}. Ignoring.", orderId);
            return;
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        // Simulate payment processing (replace with Razorpay/Stripe in production)
        boolean paymentSuccess = simulatePayment(amount);

        Payment payment = Payment.builder()
                .transactionId(transactionId)
                .orderId(orderId)
                .orderNumber(orderNumber)
                .patientId(patientId)
                .patientEmail(patientEmail)
                .amount(amount)
                .status(paymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .paymentMethod(PaymentMethod.UPI)
                .processedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Publish result back → Pharmacy + Notification services
        Map<String, Object> resultEvent = new HashMap<>();
        resultEvent.put("orderId", orderId);
        resultEvent.put("orderNumber", orderNumber);
        resultEvent.put("patientId", patientId);
        resultEvent.put("patientEmail", patientEmail);
        resultEvent.put("transactionId", transactionId);
        resultEvent.put("amount", amount);
        resultEvent.put("paymentStatus", paymentSuccess ? "SUCCESS" : "FAILED");
        resultEvent.put("paymentTime", LocalDateTime.now().toString());

        kafkaTemplate.send(paymentCompletedTopic, orderNumber, resultEvent);
        log.info("PaymentCompletedEvent published: order={} | status={}",
                orderNumber, paymentSuccess ? "SUCCESS" : "FAILED");
    }

    /**
     * Simulates payment processing.
     * Replace with actual Razorpay / Stripe SDK call in production.
     * Currently: 90% success rate for realistic simulation.
     */
    private boolean simulatePayment(BigDecimal amount) {
        return Math.random() > 0.1;  // 90% success
    }
}
