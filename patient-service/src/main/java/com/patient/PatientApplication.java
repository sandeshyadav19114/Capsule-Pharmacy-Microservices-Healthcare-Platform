package com.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Patient Service — manages patient profiles and medical history.
 * Port: 8084  |  Eureka: PATIENT-SERVICE
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PatientApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatientApplication.class, args);
    }
}
