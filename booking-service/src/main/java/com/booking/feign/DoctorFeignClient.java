package com.booking.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Feign client to call DOCTOR-SERVICE for:
 *  - Fetching doctor details (name, fee, specialization)
 *  - Checking doctor availability
 *
 * @FeignClient name must match the Eureka service name of DOCTOR-SERVICE.
 * Circuit breaker fallback: DoctorFeignFallback
 */
@FeignClient(name = "DOCTOR-SERVICE", fallback = DoctorFeignFallback.class)
public interface DoctorFeignClient {

    @GetMapping("/api/doctors/{id}")
    Map<String, Object> getDoctorById(@PathVariable Long id);

    @PutMapping("/api/doctors/{id}/availability")
    Map<String, Object> setAvailability(@PathVariable Long id,
                                         @RequestParam boolean available);
}
