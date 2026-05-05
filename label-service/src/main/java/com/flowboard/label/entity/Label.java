package com.flowboard.label.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "labels")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Label {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long boardId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String color;
    @CreationTimestamp @Column(updatable = false) private LocalDateTime createdAt;
}