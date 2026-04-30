package com.pharmacy.repository;

import com.pharmacy.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientIdOrderByUploadedAtDesc(Long patientId);
    List<Prescription> findByPatientIdAndVerifiedTrue(Long patientId);
    Optional<Prescription> findByIdAndPatientId(Long id, Long patientId);
}
