package com.flowboard.notification.service.impl;

import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.exception.ResourceNotFoundException;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor @Transactional
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    @Override
    public Notification send(NotificationRequest request) {
        Notification n = Notification.builder()
                .recipientId(request.recipientId()).actorId(request.actorId()).type(request.type())
                .title(request.title()).message(request.message()).relatedId(request.relatedId())
                .relatedType(request.relatedType()).deepLinkUrl(request.deepLinkUrl()).build();
        Notification saved = notificationRepository.save(n);
        log.info("Notification sent to user {}: {}", request.recipientId(), request.title());
        return saved;
    }

    @Override
    public void sendBulk(List<Long> recipientIds, Notification.NotificationType type, String title, String message) {
        recipientIds.forEach(rid -> send(new NotificationRequest(rid, null, type, title, message, null, null, null)));
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        n.setIsRead(true);
        notificationRepository.save(n);
    }

    @Override
    public void markAllRead(Long userId) {
        notificationRepository.findByRecipientIdAndIsRead(userId, false)
                .forEach(n -> { n.setIsRead(true); notificationRepository.save(n); });
    }

    @Override
    public void deleteRead(Long userId) {
        notificationRepository.deleteByRecipientIdAndIsReadTrue(userId);
    }

    @Override @Transactional(readOnly = true)
    public List<Notification> getByRecipient(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Override @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsRead(userId, false);
    }

    @Override
    public void delete(Long id, Long userId) { notificationRepository.deleteById(id); }
}
