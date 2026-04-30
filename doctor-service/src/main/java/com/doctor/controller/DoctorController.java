package com.doctor.controller;

import com.doctor.entity.Doctor;
import com.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * DoctorController — manages doctor profiles.
 *
 * GET  /api/doctors                     → list all available (public)
 * GET  /api/doctors/{id}                → get by ID (public)
 * GET  /api/doctors/specialization/{s}  → filter (public)
 * GET  /api/doctors/search?name=        → search (public)
 * POST /api/doctors/profile             → create profile (DOCTOR role)
 * PUT  /api/doctors/{id}                → update profile (DOCTOR role)
 * PUT  /api/doctors/{id}/availability   → toggle availability (DOCTOR)
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Doctor profiles and availability")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    @Operation(summary = "Get all available doctors")
    public ResponseEntity<List<Doctor>> getAll() {
        return ResponseEntity.ok(doctorService.getAllAvailableDoctors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID")
    public ResponseEntity<Doctor> getById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @GetMapping("/specialization/{specialization}")
    @Operation(summary = "Get doctors by specialization")
    public ResponseEntity<List<Doctor>> getBySpecialization(@PathVariable String specialization) {
        return ResponseEntity.ok(doctorService.getDoctorsBySpecialization(specialization));
    }

    @GetMapping("/search")
    @Operation(summary = "Search doctors by name")
    public ResponseEntity<List<Doctor>> search(@RequestParam String name) {
        return ResponseEntity.ok(doctorService.searchDoctors(name));
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Create doctor profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Doctor> createProfile(@RequestBody Doctor doctor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createProfile(doctor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Update doctor profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Doctor> update(@PathVariable Long id, @RequestBody Doctor doctor) {
        return ResponseEntity.ok(doctorService.updateProfile(id, doctor));
    }

    @PutMapping("/{id}/availability")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Toggle availability", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Doctor> setAvailability(@PathVariable Long id,
                                                   @RequestParam boolean available) {
        return ResponseEntity.ok(doctorService.setAvailability(id, available));
    }
}
