package com.flowboard.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BoardMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.MEMBER;

    @CreationTimestamp
    private LocalDateTime addedAt;

    public enum Role { OBSERVER, MEMBER, ADMIN }
}