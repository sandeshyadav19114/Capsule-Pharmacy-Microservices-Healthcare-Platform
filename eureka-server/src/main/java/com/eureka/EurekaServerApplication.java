package com.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Service Registry — central service discovery for all microservices.
 *
 * All services register here on startup using @EnableDiscoveryClient.
 * API Gateway uses Eureka to dynamically resolve service locations
 * for load balancing without hardcoded URLs.
 *
 * Dashboard: http://localhost:8761
 *
 * Startup order:
 *   1. eureka-server   (port 8761)
 *   2. config-server   (port 8888)
 *   3. api-gateway     (port 8080)
 *   4. All other services
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
