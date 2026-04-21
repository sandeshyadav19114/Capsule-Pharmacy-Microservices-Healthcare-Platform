package com.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway — single entry point for all microservices.
 *
 * Responsibilities:
 *  - Routes requests to downstream services via Eureka service discovery
 *  - JWT validation on every protected route (AuthFilter)
 *  - Global CORS handling
 *  - Load balancing via lb:// URIs (Spring Cloud LoadBalancer)
 *
 * Port: 8080
 * All services accessible via: http://localhost:8080/api/{service-path}
 *
 * Examples:
 *   POST http://localhost:8080/api/auth/login          → USER-SERVICE
 *   GET  http://localhost:8080/api/doctors             → DOCTOR-SERVICE
 *   POST http://localhost:8080/api/appointments/book   → BOOKING-SERVICE
 *   POST http://localhost:8080/api/cart/add            → PHARMACY-SERVICE
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
