package com.flowboard.comment.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "comments")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long cardId;
    @Column(nullable = false) private Long authorId;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    private Long parentCommentId;
    @Builder.Default private Boolean isDeleted = false;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}