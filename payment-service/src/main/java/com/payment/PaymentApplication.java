package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Payment Service — processes payments for pharmacy orders.
 *
 * Features:
 *  - Kafka consumer: listens on order-placed-topic from Pharmacy Service
 *  - Processes payment (simulated / Razorpay integration ready)
 *  - Kafka producer: publishes payment-completed-topic → Pharmacy + Notification services
 *  - Payment history stored in DB
 *
 * Port: 8087  |  Eureka: PAYMENT-SERVICE
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
