package com.flowboard.notification.repository;
import com.flowboard.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    List<Notification> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
    void deleteByRecipientIdAndIsReadTrue(Long recipientId);
}