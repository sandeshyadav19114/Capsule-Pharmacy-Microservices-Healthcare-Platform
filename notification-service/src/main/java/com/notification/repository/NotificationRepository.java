package com.notification.repository;

import com.notification.entity.Notification;
import com.notification.enums.NotificationStatus;
import com.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetries);

    List<Notification> findByTypeAndStatus(NotificationType type, NotificationStatus status);

    // For reminder scheduler: find all pending reminders not yet sent
    @Query("SELECT n FROM Notification n WHERE n.type = 'APPOINTMENT_REMINDER' " +
           "AND n.status = 'PENDING' AND n.createdAt <= :cutoff")
    List<Notification> findPendingReminders(@Param("cutoff") LocalDateTime cutoff);

    // Count sent notifications today (for monitoring/rate limiting)
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = 'SENT' " +
           "AND n.sentAt >= :startOfDay")
    Long countSentToday(@Param("startOfDay") LocalDateTime startOfDay);
}
