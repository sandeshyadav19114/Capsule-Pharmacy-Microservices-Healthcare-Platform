package com.booking.service;

import com.booking.entity.Appointment;
import com.booking.enums.AppointmentStatus;
import com.booking.feign.DoctorFeignClient;
import com.booking.kafka.BookingKafkaProducer;
import com.booking.repository.AppointmentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * BookingService — core appointment booking logic.
 *
 * Uses Resilience4j @CircuitBreaker on Doctor Service calls.
 * If DOCTOR-SERVICE is down, circuit opens and fallback is returned.
 *
 * Kafka events published:
 *   - appointment-booked-topic    → Notification sends confirmation email
 *   - appointment-cancelled-topic → Notification sends cancellation email
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorFeignClient doctorFeignClient;
    private final BookingKafkaProducer kafkaProducer;

    @CircuitBreaker(name = "doctorService", fallbackMethod = "bookAppointmentFallback")
    public Appointment bookAppointment(Long patientId, String patientEmail, String patientName,
                                       Long doctorId, LocalDateTime appointmentTime,
                                       String appointmentType, String symptoms) {

        // Fetch doctor details via Feign (with circuit breaker)
        Map<String, Object> doctor = doctorFeignClient.getDoctorById(doctorId);

        if (Boolean.FALSE.equals(doctor.get("available"))) {
            throw new IllegalStateException("Doctor is currently unavailable.");
        }

        // Check for conflicting bookings (±30 min window)
        boolean isBooked = appointmentRepository.isDoctorBooked(
                doctorId,
                appointmentTime.minusMinutes(30),
                appointmentTime.plusMinutes(30)
        );
        if (isBooked) {
            throw new IllegalStateException("Doctor already has an appointment at this time. Please choose a different slot.");
        }

        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .patientEmail(patientEmail)
                .patientName(patientName)
                .doctorId(doctorId)
                .doctorName((String) doctor.getOrDefault("fullName", "Doctor"))
                .specialization((String) doctor.getOrDefault("specialization", "General"))
                .appointmentTime(appointmentTime)
                .appointmentType(com.booking.enums.AppointmentType.valueOf(
                    appointmentType != null ? appointmentType : "IN_PERSON"))
                .status(AppointmentStatus.CONFIRMED)
                .symptoms(symptoms)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        // Publish Kafka event → Notification Service
        kafkaProducer.publishAppointmentBooked(
                saved.getId(), patientId, patientEmail, patientName,
                doctorId, saved.getDoctorName(), saved.getSpecialization(),
                appointmentTime, saved.getAppointmentType().name()
        );

        log.info("Appointment booked: id={} | patient={} | doctor={} | time={}",
                saved.getId(), patientId, doctorId, appointmentTime);
        return saved;
    }

    public Appointment bookAppointmentFallback(Long patientId, String patientEmail,
                                                String patientName, Long doctorId,
                                                LocalDateTime appointmentTime,
                                                String appointmentType, String symptoms,
                                                Throwable t) {
        log.warn("Circuit breaker OPEN for doctorService — bookAppointment fallback. Error: {}", t.getMessage());
        throw new RuntimeException("Doctor service is temporarily unavailable. Please try again in a few minutes.");
    }

    public Appointment cancelAppointment(Long appointmentId, Long patientId, String reason) {
        Appointment appt = appointmentRepository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + appointmentId));

        if (appt.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed appointment.");
        }

        appt.setStatus(AppointmentStatus.CANCELLED);
        appt.setCancellationReason(reason);
        Appointment saved = appointmentRepository.save(appt);

        kafkaProducer.publishAppointmentCancelled(
                saved.getId(), patientId, saved.getPatientEmail(),
                saved.getPatientName(), saved.getDoctorName(),
                saved.getAppointmentTime(), reason
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentTimeDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentTimeDesc(doctorId);
    }

    @Transactional(readOnly = true)
    public Appointment getById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));
    }

    public Appointment updateStatus(Long id, AppointmentStatus status, String notes) {
        Appointment appt = getById(id);
        appt.setStatus(status);
        if (notes != null) appt.setDoctorNotes(notes);
        return appointmentRepository.save(appt);
    }
}
