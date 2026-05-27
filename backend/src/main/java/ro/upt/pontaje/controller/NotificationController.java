package ro.upt.pontaje.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ro.upt.pontaje.model.Notification;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller pentru notificări in-app
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Returnează toate notificările utilizatorului curent
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMyNotifications(@AuthenticationPrincipal User user) {
        List<NotificationDto> dtos = notificationService.getForUser(user)
                .stream()
                .map(NotificationDto::fromEntity)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Returnează numărul de notificări necitite
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Marchează o notificare ca citită
     * PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        Notification notification = notificationService.markAsRead(id, user);
        return ResponseEntity.ok(NotificationDto.fromEntity(notification));
    }

    /**
     * Marchează toate notificările ca citite
     * PATCH /api/notifications/read-all
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.noContent().build();
    }

    /**
     * DTO pentru notificări
     */
    public record NotificationDto(
            String id,
            String type,
            String typeDisplay,
            String subject,
            String message,
            boolean isRead,
            String createdAt
    ) {
        public static NotificationDto fromEntity(Notification n) {
            return new NotificationDto(
                    n.getId().toString(),
                    n.getNotificationType().name(),
                    n.getNotificationType().getDisplayName(),
                    n.getSubject(),
                    n.getMessage(),
                    Boolean.TRUE.equals(n.getIsRead()),
                    n.getCreatedAt() != null ? n.getCreatedAt().toString() : LocalDateTime.now().toString()
            );
        }
    }
}
