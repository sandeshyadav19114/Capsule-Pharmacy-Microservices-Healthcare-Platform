package com.pharmacy.service;

import com.pharmacy.entity.Prescription;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.openai.OpenAiPrescriptionService;
import com.pharmacy.openai.PrescriptionOcrResult;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PrescriptionService — orchestrates the full prescription upload flow:
 *
 * 1. Validate file (type, size)
 * 2. Upload image to AWS S3
 * 3. Generate pre-signed URL for OpenAI
 * 4. Call OpenAI Vision API → extract medicine names
 * 5. Save Prescription entity with extracted medicines
 * 6. Return extracted medicines so patient can add them to cart
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final AwsS3Service awsS3Service;
    private final OpenAiPrescriptionService openAiService;

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/jpg", "application/pdf");

    public Prescription uploadAndAnalyzePrescription(MultipartFile file,
                                                     Long patientId,
                                                     Long doctorId) throws IOException {
        // 1. Validate
        validateFile(file);
        log.info("Processing prescription upload for patientId={}", patientId);

        // 2. Upload to S3
        String s3Key = awsS3Service.uploadPrescriptionImage(file, patientId);

        // 3. Generate pre-signed URL for OpenAI Vision
        String presignedUrl = awsS3Service.generatePresignedUrl(s3Key);

        // 4. Call OpenAI Vision API
        PrescriptionOcrResult ocrResult = openAiService.extractMedicinesFromPrescription(presignedUrl);

        // 5. Build and save prescription
        String extractedMedicinesStr = "";
        if (ocrResult.isSuccess() && ocrResult.getMedicineNames() != null) {
            extractedMedicinesStr = String.join(",", ocrResult.getMedicineNames());
        }

        Prescription prescription = Prescription.builder()
                .patientId(patientId)
                .doctorId(doctorId != null ? doctorId : 0L)
                .s3ImageKey(s3Key)
                .s3ImageUrl(presignedUrl)
                .extractedMedicines(extractedMedicinesStr)
                .openAiRawResponse(ocrResult.getRawOpenAiResponse())
                .verified(false)
                .active(true)
                .build();

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Prescription saved: id={} | extracted medicines: {}", saved.getId(), extractedMedicinesStr);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Prescription> getPatientPrescriptions(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByUploadedAtDesc(patientId);
    }

    @Transactional(readOnly = true)
    public Prescription getPrescriptionById(Long id, Long patientId) {
        return prescriptionRepository.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
    }

    /**
     * Admin/Pharmacist verifies a prescription manually.
     * Once verified=true, prescription-only medicines can be added to cart.
     */
    public Prescription verifyPrescription(Long id, boolean verified, String adminUsername, String notes) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
        prescription.setVerified(verified);
        prescription.setVerifiedAt(LocalDateTime.now());
        prescription.setVerifiedBy(adminUsername);
        prescription.setNotes(notes);
        log.info("Prescription {} {} by {}", id, verified ? "VERIFIED" : "REJECTED", adminUsername);
        return prescriptionRepository.save(prescription);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Allowed: JPEG, PNG, PDF");
        }
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException("File too large. Max size: 10MB");
        }
    }

        @Transactional(readOnly = true)
        public List<Prescription> getAllUnverified () {
            return prescriptionRepository.findAll().stream()
                    .filter(p -> !p.getVerified())
                    .collect(java.util.stream.Collectors.toList());
        }
    }
