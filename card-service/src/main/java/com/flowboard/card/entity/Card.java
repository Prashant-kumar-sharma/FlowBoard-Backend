package com.flowboard.card.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "cards")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Card {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long listId;
    @Column(nullable = false) private Long boardId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private Integer position;
    @Enumerated(EnumType.STRING) @Builder.Default private Priority priority = Priority.MEDIUM;
    @Enumerated(EnumType.STRING) @Builder.Default private Status status = Status.TO_DO;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Long assigneeId;
    private Long createdById;
    @Builder.Default private Boolean isArchived = false;
    private String coverColor;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Status { TO_DO, IN_PROGRESS, IN_REVIEW, DONE }
}