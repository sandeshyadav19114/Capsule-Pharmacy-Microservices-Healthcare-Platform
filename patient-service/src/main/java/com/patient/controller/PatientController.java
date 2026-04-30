package com.patient.controller;

import com.patient.entity.Patient;
import com.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * PatientController — patient profile management.
 *
 * POST /api/patients/profile  → create profile (PATIENT)
 * GET  /api/patients/me       → get own profile (PATIENT)
 * PUT  /api/patients/{id}     → update profile (PATIENT)
 * GET  /api/patients/{id}     → get by ID (ADMIN or DOCTOR)
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient profile management")
public class PatientController {

    private final PatientService patientService;

    @PostMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Create patient profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Patient> createProfile(@RequestBody Patient patient,
                                                  Authentication auth) {
        Long userId = extractUserId(auth);
        patient.setUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createProfile(patient));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get own patient profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Patient> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(patientService.getByUserId(extractUserId(auth)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Operation(summary = "Get patient by ID (Doctor/Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Patient> getById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Update patient profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Patient> update(@PathVariable Long id, @RequestBody Patient patient,
                                           Authentication auth) {
        return ResponseEntity.ok(patientService.updateProfile(id, patient));
    }

    private Long extractUserId(Authentication auth) {
        Object details = ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) auth).getDetails();
        return details instanceof Long ? (Long) details : Long.parseLong(details.toString());
    }
}
