package com.doctor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Doctor Service — manages doctor profiles, availability, and schedules.
 * Port: 8082  |  Eureka: DOCTOR-SERVICE
 */
@SpringBootApplication
@EnableDiscoveryClient
public class DoctorApplication {
    public static void main(String[] args) {
        SpringApplication.run(DoctorApplication.class, args);
    }
}
