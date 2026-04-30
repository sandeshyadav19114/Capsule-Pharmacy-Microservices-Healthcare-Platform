package com.notification.service;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import com.notification.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 * Uses Mockito — no Spring context loaded (fast).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private EmailService emailService;
    @Mock private EmailTemplateBuilder templateBuilder;
    @InjectMocks private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .userId(10L)
                .recipientEmail("patient@test.com")
                .recipientName("Test Patient")
                .type(NotificationType.ORDER_PLACED)
                .subject("Order Placed — ORD-2024-001")
                .body("<html>Order body</html>")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();
    }

    @Test
    @DisplayName("Should save notification and dispatch email on order placed")
    void sendOrderPlacedNotification_savesAndDispatches() {
        when(templateBuilder.buildOrderPlacedEmail(any(), any(), any(), any()))
                .thenReturn("<html>Email</html>");
        when(notificationRepository.save(any())).thenReturn(testNotification);
        doNothing().when(emailService).sendHtmlEmail(any());

        notificationService.sendOrderPlacedNotification(
                10L, "patient@test.com", "Test Patient",
                "ORD-2024-001", new BigDecimal("500.00"),
                "123 Street, City", 1L
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(emailService).sendHtmlEmail(any(Notification.class));
    }

    @Test
    @DisplayName("Should return user notifications by userId")
    void getUserNotifications_returnsList() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(testNotification));

        List<Notification> result = notificationService.getUserNotifications(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRecipientEmail()).isEqualTo("patient@test.com");
    }

    @Test
    @DisplayName("Should return failed notifications")
    void getFailedNotifications_returnsFailed() {
        testNotification.setStatus(NotificationStatus.FAILED);
        when(notificationRepository.findByStatus(NotificationStatus.FAILED))
                .thenReturn(List.of(testNotification));

        List<Notification> result = notificationService.getFailedNotifications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    @DisplayName("Should retry failed notifications up to max 3 attempts")
    void retryFailedNotifications_retriesEligible() {
        testNotification.setStatus(NotificationStatus.FAILED);
        testNotification.setRetryCount(1); // still < 3

        when(notificationRepository.findByStatusAndRetryCountLessThan(
                NotificationStatus.FAILED, 3))
                .thenReturn(List.of(testNotification));
        when(notificationRepository.save(any())).thenReturn(testNotification);
        doNothing().when(emailService).sendHtmlEmail(any());

        notificationService.retryFailedNotifications();

        // Status should be reset to PENDING before retry
        assertThat(testNotification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(emailService).sendHtmlEmail(testNotification);
    }

    @Test
    @DisplayName("Should send appointment booked notification")
    void sendAppointmentBookedNotification_savesAndDispatches() {
        when(templateBuilder.buildAppointmentBookedEmail(any(), any(), any(), any(), any()))
                .thenReturn("<html>Appt</html>");
        when(notificationRepository.save(any())).thenReturn(testNotification);
        doNothing().when(emailService).sendHtmlEmail(any());

        notificationService.sendAppointmentBookedNotification(
                10L, "patient@test.com", "Test Patient",
                "Dr. Smith", "Cardiology",
                LocalDateTime.now().plusDays(3), "IN_PERSON", 5L
        );

        verify(notificationRepository).save(any(Notification.class));
        verify(emailService).sendHtmlEmail(any(Notification.class));
    }
}
