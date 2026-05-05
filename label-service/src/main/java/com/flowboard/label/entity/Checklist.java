package com.flowboard.label.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "checklists")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Checklist {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long cardId;
    @Column(nullable = false) private String title;
    @Builder.Default private Integer position = 0;
    @OneToMany(mappedBy = "checklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<ChecklistItem> items = new ArrayList<>();
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
}