package com.config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Config Server — centralised configuration management.
 *
 * All microservices can fetch their config from this server on startup.
 * Config files are stored in src/main/resources/config/ (native mode).
 * In production, switch to Git backend for versioned, auditable config.
 *
 * Port: 8888  |  Eureka: CONFIG-SERVER
 *
 * Access config:
 *   http://localhost:8888/{service-name}/default
 *   e.g. http://localhost:8888/PHARMACY-SERVICE/default
 *
 * Startup order:
 *   1. eureka-server → 2. config-server → 3. api-gateway → 4. all services
 */
@SpringBootApplication
@EnableConfigServer
@EnableDiscoveryClient
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
