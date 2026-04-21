package com.notification.controller;

import com.notification.entity.Notification;
import com.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * NotificationController — REST API for notification history.
 *
 * GET  /api/notifications/my        → patient's notification history (PATIENT)
 * GET  /api/notifications/failed    → list failed notifications (ADMIN)
 * POST /api/notifications/retry     → manually trigger retry (ADMIN)
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification history and management")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get notification history for current user",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication auth) {
        Long userId = extractUserId(auth);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all failed notifications (Admin only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<Notification>> getFailedNotifications() {
        return ResponseEntity.ok(notificationService.getFailedNotifications());
    }

    @PostMapping("/retry")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger retry for failed notifications (Admin only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> retryFailed() {
        notificationService.retryFailedNotifications();
        return ResponseEntity.ok(Map.of("message", "Retry triggered for failed notifications"));
    }

    private Long extractUserId(Authentication auth) {
        Object d = ((org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken) auth).getDetails();
        return d instanceof Long ? (Long) d : Long.parseLong(d.toString());
    }
}
