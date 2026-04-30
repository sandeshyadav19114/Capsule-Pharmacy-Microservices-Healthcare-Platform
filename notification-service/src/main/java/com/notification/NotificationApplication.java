package com.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Notification Microservice Entry Point
 *
 * Responsibilities:
 *  - Listens to Kafka events from Booking, Payment, and Pharmacy services
 *  - Sends HTML email notifications via Spring Mail (SMTP/Gmail)
 *  - Persists all notifications in DB for audit/history
 *  - Runs @Scheduled job every hour to send appointment reminders
 *    (24 hours before scheduled appointment time)
 *
 * Kafka Topics Consumed:
 *   - order-placed-topic        → "Your order has been placed"
 *   - order-confirmed-topic     → "Payment confirmed, order processing"
 *   - order-cancelled-topic     → "Order cancelled"
 *   - appointment-booked-topic  → "Appointment confirmed"
 *   - appointment-cancelled-topic → "Appointment cancelled"
 *   - appointment-reminder-topic  → triggered by Booking scheduler
 *
 * Port: 8086
 * Eureka Name: NOTIFICATION-SERVICE
 * Health: http://localhost:8086/actuator/health
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
