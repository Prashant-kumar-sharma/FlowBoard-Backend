package com.flowboard.notification.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "notifications")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long recipientId;
    private Long actorId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private NotificationType type;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String message;
    private Long relatedId;
    private String relatedType;
    private String deepLinkUrl;
    @Builder.Default private Boolean isRead = false;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;

    public enum NotificationType { ASSIGNMENT, MENTION, DUE_DATE, COMMENT, MOVE, BROADCAST }
}