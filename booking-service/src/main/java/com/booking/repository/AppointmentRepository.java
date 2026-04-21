package com.booking.repository;

import com.booking.entity.Appointment;
import com.booking.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentTimeDesc(Long patientId);
    List<Appointment> findByDoctorIdOrderByAppointmentTimeDesc(Long doctorId);
    List<Appointment> findByStatus(AppointmentStatus status);
    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);

    // Check if doctor has a conflicting booking at the same time (±30 min)
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctorId = :doctorId " +
           "AND a.status NOT IN ('CANCELLED','NO_SHOW') " +
           "AND a.appointmentTime BETWEEN :from AND :to")
    boolean isDoctorBooked(@Param("doctorId") Long doctorId,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to);

    // For reminder scheduler: appointments in next 24-25 hours, reminder not yet sent
    @Query("SELECT a FROM Appointment a WHERE a.status = 'CONFIRMED' " +
           "AND a.reminderSent = false " +
           "AND a.appointmentTime BETWEEN :from AND :to")
    List<Appointment> findAppointmentsNeedingReminder(@Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);
}
