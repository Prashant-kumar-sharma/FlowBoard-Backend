package com.flowboard.notification.controller;

import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j @RestController @RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor @Tag(name = "Notifications") @SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for the current user")
    public ResponseEntity<List<Notification>> getAll(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(notificationService.getByRecipient(userId));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String,Long>> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/read")
    @Operation(summary = "Delete all read notifications")
    public ResponseEntity<Void> deleteRead(@RequestHeader("X-User-Id") Long userId) {
        notificationService.deleteRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        notificationService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/broadcast")
    @Operation(summary = "Broadcast notification to all users - admin only")
    public ResponseEntity<Void> broadcast(
            @RequestBody com.flowboard.notification.dto.request.BroadcastRequest request,
            @RequestHeader("X-User-Role") String requesterRole) {
        if (!"PLATFORM_ADMIN".equalsIgnoreCase(requesterRole)) {
            throw new IllegalArgumentException("Platform admin access required");
        }
        notificationService.sendBulk(request.getRecipientIds(), Notification.NotificationType.BROADCAST, request.getTitle(), request.getMessage());
        return ResponseEntity.noContent().build();
    }
}
