package com.patient.service;

import com.patient.entity.Patient;
import com.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    public Patient createProfile(Patient patient) {
        if (patientRepository.findByUserId(patient.getUserId()).isPresent()) {
            throw new IllegalArgumentException("Profile already exists for userId: " + patient.getUserId());
        }
        return patientRepository.save(patient);
    }

    public Patient updateProfile(Long id, Patient updated) {
        Patient existing = getById(id);
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setCity(updated.getCity());
        existing.setPincode(updated.getPincode());
        existing.setAllergies(updated.getAllergies());
        existing.setChronicConditions(updated.getChronicConditions());
        existing.setEmergencyContactName(updated.getEmergencyContactName());
        existing.setEmergencyContactPhone(updated.getEmergencyContactPhone());
        return patientRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Patient getById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + id));
    }

    @Transactional(readOnly = true)
    public Patient getByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Patient profile not found for userId: " + userId));
    }
}
