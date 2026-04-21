package com.booking.controller;

import com.booking.entity.Appointment;
import com.booking.enums.AppointmentStatus;
import com.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * BookingController — REST API for appointment lifecycle.
 *
 * POST /api/appointments/book              → book appointment (PATIENT)
 * GET  /api/appointments/my               → patient's appointments (PATIENT)
 * GET  /api/appointments/doctor           → doctor's appointments (DOCTOR)
 * GET  /api/appointments/{id}             → get by ID
 * PUT  /api/appointments/{id}/cancel      → cancel (PATIENT)
 * PUT  /api/appointments/{id}/status      → update status (DOCTOR/ADMIN)
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Book and manage doctor appointments")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/book")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Book appointment with doctor", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Appointment> book(
            @RequestParam @NotNull Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentTime,
            @RequestParam(defaultValue = "IN_PERSON") String appointmentType,
            @RequestParam(required = false) String symptoms,
            @RequestParam String patientEmail,
            @RequestParam String patientName,
            Authentication auth) {

        Long patientId = extractUserId(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingService.bookAppointment(patientId, patientEmail, patientName,
                        doctorId, appointmentTime, appointmentType, symptoms));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get patient's appointments", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Appointment>> getMyAppointments(Authentication auth) {
        return ResponseEntity.ok(bookingService.getPatientAppointments(extractUserId(auth)));
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Get doctor's appointments", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Appointment>> getDoctorAppointments(Authentication auth) {
        return ResponseEntity.ok(bookingService.getDoctorAppointments(extractUserId(auth)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "Get appointment by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Appointment> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    @Operation(summary = "Cancel appointment", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Appointment> cancel(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Cancelled by patient") String reason,
            Authentication auth) {
        return ResponseEntity.ok(bookingService.cancelAppointment(id, extractUserId(auth), reason));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    @Operation(summary = "Update appointment status (Doctor/Admin)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Appointment> updateStatus(
            @PathVariable Long id,
            @RequestParam AppointmentStatus status,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(bookingService.updateStatus(id, status, notes));
    }

    private Long extractUserId(Authentication auth) {
        Object d = ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) auth).getDetails();
        return d instanceof Long ? (Long) d : Long.parseLong(d.toString());
    }
}
