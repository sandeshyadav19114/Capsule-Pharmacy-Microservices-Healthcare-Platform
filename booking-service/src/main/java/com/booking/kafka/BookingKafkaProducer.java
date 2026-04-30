package com.booking.kafka;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.appointment-booked}")
    private String appointmentBookedTopic;

    @Value("${kafka.topics.appointment-cancelled}")
    private String appointmentCancelledTopic;

    @Value("${kafka.topics.appointment-reminder}")
    private String appointmentReminderTopic;

    public void publishAppointmentBooked(Long appointmentId, Long patientId,
                                          String patientEmail, String patientName,
                                          Long doctorId, String doctorName, String specialization,
                                          LocalDateTime apptTime, String apptType) {
        var event = new java.util.HashMap<String, Object>();
        event.put("appointmentId", appointmentId);
        event.put("patientId", patientId);
        event.put("patientEmail", patientEmail);
        event.put("patientName", patientName);
        event.put("doctorId", doctorId);
        event.put("doctorName", doctorName);
        event.put("specialization", specialization);
        event.put("appointmentTime", apptTime.toString());
        event.put("appointmentType", apptType);
        kafkaTemplate.send(appointmentBookedTopic, String.valueOf(appointmentId), event);
        log.info("Published AppointmentBookedEvent: id={}", appointmentId);
    }

    public void publishAppointmentCancelled(Long appointmentId, Long patientId,
                                             String patientEmail, String patientName,
                                             String doctorName, LocalDateTime apptTime,
                                             String reason) {
        var event = new java.util.HashMap<String, Object>();
        event.put("appointmentId", appointmentId);
        event.put("patientId", patientId);
        event.put("patientEmail", patientEmail);
        event.put("patientName", patientName);
        event.put("doctorName", doctorName);
        event.put("appointmentTime", apptTime.toString());
        event.put("cancellationReason", reason);
        kafkaTemplate.send(appointmentCancelledTopic, String.valueOf(appointmentId), event);
        log.info("Published AppointmentCancelledEvent: id={}", appointmentId);
    }

    public void publishAppointmentReminder(Long appointmentId, Long patientId,
                                            String patientEmail, String patientName,
                                            String doctorName, String specialization,
                                            LocalDateTime apptTime, String apptType) {
        var event = new java.util.HashMap<String, Object>();
        event.put("appointmentId", appointmentId);
        event.put("patientId", patientId);
        event.put("patientEmail", patientEmail);
        event.put("patientName", patientName);
        event.put("doctorName", doctorName);
        event.put("specialization", specialization);
        event.put("appointmentTime", apptTime.toString());
        event.put("appointmentType", apptType);
        kafkaTemplate.send(appointmentReminderTopic, String.valueOf(appointmentId), event);
        log.info("Published AppointmentReminderEvent: id={}", appointmentId);
    }
}
