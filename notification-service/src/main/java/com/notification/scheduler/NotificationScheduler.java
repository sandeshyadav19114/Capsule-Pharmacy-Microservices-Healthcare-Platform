package com.notification.scheduler;

import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * NotificationScheduler — periodic background jobs.
 *
 * Jobs:
 *  1. retryFailedNotifications()  — runs every 30 min
 *     Retries all FAILED notifications (max 3 attempts each).
 *
 *  2. logDailyStats()             — runs at midnight
 *     Logs how many notifications were sent today (monitoring).
 *
 * Note: Appointment reminders are triggered via Kafka from BookingService
 * scheduler (AppointmentReminderScheduler), NOT from here.
 * This service just consumes those Kafka events and sends the emails.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final NotificationService notificationService;

    /**
     * Retry FAILED notifications every 30 minutes.
     * Only retries notifications that have been attempted < 3 times.
     */
    @Scheduled(fixedDelay = 1800000) // 30 minutes
    public void retryFailedNotifications() {
        log.info("Running retry scheduler for failed notifications...");
        try {
            notificationService.retryFailedNotifications();
        } catch (Exception e) {
            log.error("Retry scheduler failed: {}", e.getMessage());
        }
    }

    /**
     * Log daily notification stats at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void logDailyStats() {
        log.info("Daily notification report: {} notifications sent today",
                notificationService.countSentToday());
    }
}
