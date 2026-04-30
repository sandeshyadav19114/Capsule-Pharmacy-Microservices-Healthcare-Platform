package com.notification.service;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * EmailService — sends HTML emails using JavaMailSender (SMTP).
 *
 * Features:
 *  - Async sending (@Async) — does not block Kafka consumer thread
 *  - Retry on failure (@Retryable) — retries 3 times with exponential backoff
 *  - Persists notification status (SENT / FAILED) for audit
 *  - HTML emails with inline styling (no external CSS dependencies)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    /**
     * Sends an HTML email asynchronously.
     * Marks Notification as SENT or FAILED in DB.
     */
    @Async
    @Retryable(
        retryFor = MessagingException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendHtmlEmail(Notification notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getBody(), true); // true = isHtml

            mailSender.send(message);

            // Mark as SENT
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Email sent successfully to: {} | type: {} | id: {}",
                    notification.getRecipientEmail(),
                    notification.getType(),
                    notification.getId());

        } catch (Exception e) {
            log.error("Failed to send email to: {} | error: {}",
                    notification.getRecipientEmail(), e.getMessage());

            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);
        }
    }
}
