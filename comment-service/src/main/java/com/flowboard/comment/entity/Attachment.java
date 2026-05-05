package com.flowboard.comment.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "attachments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Attachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long cardId;
    @Column(nullable = false) private Long uploaderId;
    @Column(nullable = false) private String fileName;
    @Column(nullable = false) private String fileUrl;
    private String fileType;
    private Long sizeKb;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime uploadedAt;
}