package com.pharmacy.controller;

import com.pharmacy.config.JwtUtil;
import com.pharmacy.entity.Prescription;

import com.pharmacy.service.PrescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PrescriptionController — REST API for prescription upload and management.
 *
 * Patient:
 *   POST /api/prescriptions/upload              → upload image → S3 → OpenAI OCR
 *   GET  /api/prescriptions/my                  → list patient's prescriptions
 *   GET  /api/prescriptions/{id}                → get single prescription
 *
 * Admin/Pharmacist:
 *   GET  /api/prescriptions/pending             → list unverified prescriptions
 *   PUT  /api/prescriptions/{id}/verify         → verify or reject
 */
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Upload and manage prescriptions with AI-powered OCR")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final JwtUtil jwtUtil;

    /**
     * Upload a prescription image.
     * Triggers: S3 upload → OpenAI Vision OCR → save extracted medicines.
     *
     * @param file         the prescription image (JPEG/PNG/PDF)
     * @param doctorId     optional doctor ID if prescribed by a known doctor
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Upload prescription image — triggers OpenAI OCR",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<?> uploadPrescription(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long doctorId,
            Authentication authentication) {

        Long patientId = extractPatientId(authentication);

        try {
            Prescription prescription = prescriptionService
                    .uploadAndAnalyzePrescription(file, patientId, doctorId);

            // Return the extracted medicine names directly for UX
            List<String> medicines = prescription.getExtractedMedicines() != null
                    ? Arrays.asList(prescription.getExtractedMedicines().split(","))
                    : List.of();

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "prescriptionId",       prescription.getId(),
                    "imageUrl",             prescription.getS3ImageUrl(),
                    "extractedMedicines",   medicines,
                    "verified",             prescription.getVerified(),
                    "message", medicines.isEmpty()
                            ? "Prescription uploaded. No medicines could be extracted automatically."
                            : "Prescription uploaded successfully. " + medicines.size() + " medicines extracted."
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "File upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get all prescriptions for current patient",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Prescription>> getMyPrescriptions(Authentication authentication) {
        Long patientId = extractPatientId(authentication);
        return ResponseEntity.ok(prescriptionService.getPatientPrescriptions(patientId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN')")
    @Operation(summary = "Get prescription by ID",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Prescription> getPrescriptionById(@PathVariable Long id,
                                                             Authentication authentication) {
        Long patientId = extractPatientId(authentication);
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id, patientId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List unverified prescriptions for pharmacist review",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Prescription>> getPendingVerification() {
        // Return all prescriptions where verified=false
        return ResponseEntity.ok(
                prescriptionService.getAllUnverified());
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Verify or reject a prescription (Admin/Pharmacist only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Prescription> verifyPrescription(
            @PathVariable Long id,
            @RequestParam boolean verified,
            @RequestParam(required = false) String notes,
            Authentication authentication) {

        String adminUsername = authentication.getName();
        return ResponseEntity.ok(
                prescriptionService.verifyPrescription(id, verified, adminUsername, notes));
    }

    // Extract patientId stored in JWT details by JwtAuthFilter
    private Long extractPatientId(Authentication authentication) {
        Object details = ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) authentication).getDetails();
        if (details instanceof Long) return (Long) details;
        if (details instanceof Integer) return ((Integer) details).longValue();
        return Long.parseLong(details.toString());
    }
}
