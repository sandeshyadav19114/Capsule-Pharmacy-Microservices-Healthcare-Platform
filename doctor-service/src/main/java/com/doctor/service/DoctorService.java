package com.doctor.service;

import com.doctor.entity.Doctor;
import com.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public Doctor createProfile(Doctor doctor) {
        log.info("Creating doctor profile for userId={}", doctor.getUserId());
        return doctorRepository.save(doctor);
    }

    public Doctor updateProfile(Long id, Doctor updated) {
        Doctor existing = getById(id);
        existing.setSpecialization(updated.getSpecialization());
        existing.setQualification(updated.getQualification());
        existing.setHospital(updated.getHospital());
        existing.setConsultationFee(updated.getConsultationFee());
        existing.setAvailableDays(updated.getAvailableDays());
        existing.setAvailableTimeFrom(updated.getAvailableTimeFrom());
        existing.setAvailableTimeTo(updated.getAvailableTimeTo());
        return doctorRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Doctor getById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + id));
    }

    @Transactional(readOnly = true)
    public Doctor getByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found for userId: " + userId));
    }

    @Transactional(readOnly = true)
    public List<Doctor> getAllAvailableDoctors() {
        return doctorRepository.findByAvailableTrueAndActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecializationIgnoreCaseAndAvailableTrue(specialization);
    }

    @Transactional(readOnly = true)
    public List<Doctor> searchDoctors(String name) {
        return doctorRepository.searchByName(name);
    }

    public Doctor setAvailability(Long id, boolean available) {
        Doctor doctor = getById(id);
        doctor.setAvailable(available);
        return doctorRepository.save(doctor);
    }
}
