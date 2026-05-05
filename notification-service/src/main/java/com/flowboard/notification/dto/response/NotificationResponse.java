package com.flowboard.notification.dto.response;
import com.flowboard.notification.entity.Notification;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationResponse {
    private Long id; private Long recipientId; private Long actorId; private String type;
    private String title; private String message; private Long relatedId; private String relatedType;
    private String deepLinkUrl; private Boolean isRead; private LocalDateTime createdAt;
    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder().id(n.getId()).recipientId(n.getRecipientId())
            .actorId(n.getActorId()).type(n.getType().name()).title(n.getTitle()).message(n.getMessage())
            .relatedId(n.getRelatedId()).relatedType(n.getRelatedType()).deepLinkUrl(n.getDeepLinkUrl())
            .isRead(n.getIsRead()).createdAt(n.getCreatedAt()).build();
    }
}