package com.flowboard.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_audit_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long boardId;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private String action;

    private String targetType;

    private String targetId;

    @Column(length = 1000)
    private String details;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
