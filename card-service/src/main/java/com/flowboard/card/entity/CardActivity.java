package com.flowboard.card.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "card_activities")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CardActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long cardId;
    @Column(nullable = false) private Long actorId;
    @Column(nullable = false) private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
}