package com.flowboard.board.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "boards")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Board {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    private String description;

    @Column(nullable = false)
    private Long workspaceId;

    @Column(nullable = false)
    private Long createdById;

    @Builder.Default
    private String background = "#0079BF";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Builder.Default
    private Boolean isClosed = false;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoardMember> members = new ArrayList<>();

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Visibility { PUBLIC, PRIVATE }
}