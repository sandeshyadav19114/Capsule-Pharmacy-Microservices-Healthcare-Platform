package com.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * User / Auth Service — handles registration, login, JWT issuance.
 * Port: 8081  |  Eureka: USER-LOGIN-SIGNUP
 * Swagger: http://localhost:8081/swagger-ui.html
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserLoginSignupApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserLoginSignupApplication.class, args);
    }
}
