package com.booking.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Fallback for DoctorFeignClient — returned when DOCTOR-SERVICE is
 * unreachable or circuit breaker is OPEN.
 * Returns a safe default response rather than propagating the failure.
 */
@Component
@Slf4j
public class DoctorFeignFallback implements DoctorFeignClient {

    @Override
    public Map<String, Object> getDoctorById(Long id) {
        log.warn("DoctorService unavailable — returning fallback for doctorId={}", id);
        return Map.of("id", id, "fullName", "Doctor (unavailable)", "available", false,
                      "fallback", true);
    }

    @Override
    public Map<String, Object> setAvailability(Long id, boolean available) {
        log.warn("DoctorService unavailable — cannot update availability for doctorId={}", id);
        return Map.of("id", id, "fallback", true);
    }
}
