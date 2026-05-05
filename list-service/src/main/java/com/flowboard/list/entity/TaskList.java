package com.flowboard.list.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_lists")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TaskList {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private Integer position;

    @Builder.Default
    private String color = "#E2E4E9";

    @Builder.Default
    private Boolean isArchived = false;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}