package com.pharmacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Pharmacy Microservice Entry Point
 *
 * Responsibilities:
 *  - Medicine catalog management (CRUD)
 *  - Prescription image upload to AWS S3
 *  - OpenAI GPT Vision API integration for prescription OCR
 *  - Cart management (add/remove/view)
 *  - Order placement and lifecycle management
 *  - Kafka producer: publishes order-placed events
 *  - Kafka consumer: listens for payment-completed events
 *  - Resilience4j circuit breaker on Payment Service calls
 *
 * Port: 8085
 * Eureka Name: PHARMACY-SERVICE
 * Swagger UI: http://localhost:8085/swagger-ui.html
 * Health: http://localhost:8085/actuator/health
 * Circuit Breakers: http://localhost:8085/actuator/circuitbreakers
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class PharmacyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PharmacyApplication.class, args);
    }
}
