package com.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Booking Service — appointment booking, scheduling, and reminders.
 *
 * Features:
 *  - Book/cancel/reschedule appointments with doctors
 *  - Feign client to validate doctor availability (DOCTOR-SERVICE)
 *  - Resilience4j circuit breaker on Doctor Service calls
 *  - Kafka producer: appointment-booked, appointment-cancelled events
 *  - @Scheduled job: publishes reminder events 24h before appointments
 *
 * Port: 8083  |  Eureka: BOOKING-SERVICE
 * Actuator: http://localhost:8083/actuator/health
 * Circuit Breakers: http://localhost:8083/actuator/circuitbreakers
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class BookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingApplication.class, args);
    }
}
