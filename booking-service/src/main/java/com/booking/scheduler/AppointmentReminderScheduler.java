package com.booking.scheduler;

import com.booking.entity.Appointment;
import com.booking.kafka.BookingKafkaProducer;
import com.booking.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AppointmentReminderScheduler — runs every hour to check for upcoming appointments
 * and publishes reminder events to Kafka (consumed by Notification Service).
 *
 * Logic: Find all CONFIRMED appointments scheduled 24–25 hours from now
 *        where reminderSent = false → publish reminder event → set reminderSent = true
 *
 * @Scheduled cron: "0 0 * * * *" = top of every hour
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final BookingKafkaProducer kafkaProducer;

    @Scheduled(cron = "0 0 * * * *")   // every hour
    @Transactional
    public void sendAppointmentReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(24);
        LocalDateTime to   = LocalDateTime.now().plusHours(25);

        List<Appointment> upcoming = appointmentRepository
                .findAppointmentsNeedingReminder(from, to);

        log.info("Reminder scheduler: found {} appointments needing reminders", upcoming.size());

        for (Appointment appt : upcoming) {
            try {
                kafkaProducer.publishAppointmentReminder(
                        appt.getId(),
                        appt.getPatientId(),
                        appt.getPatientEmail(),
                        appt.getPatientName(),
                        appt.getDoctorName(),
                        appt.getSpecialization(),
                        appt.getAppointmentTime(),
                        appt.getAppointmentType().name()
                );
                appt.setReminderSent(true);
                appointmentRepository.save(appt);
            } catch (Exception e) {
                log.error("Failed to send reminder for appointmentId={}: {}", appt.getId(), e.getMessage());
            }
        }
    }
}
