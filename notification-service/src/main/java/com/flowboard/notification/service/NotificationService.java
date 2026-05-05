package com.flowboard.notification.service;
import com.flowboard.notification.entity.Notification;
import java.util.List;
public interface NotificationService {
    Notification send(NotificationRequest request);
    void sendBulk(List<Long> recipientIds, Notification.NotificationType type, String title, String message);
    void markAsRead(Long id, Long userId);
    void markAllRead(Long userId);
    void deleteRead(Long userId);
    List<Notification> getByRecipient(Long userId);
    long getUnreadCount(Long userId);
    void delete(Long id, Long userId);

    record NotificationRequest(
            Long recipientId,
            Long actorId,
            Notification.NotificationType type,
            String title,
            String message,
            Long relatedId,
            String relatedType,
            String deepLinkUrl
    ) {
    }
}
