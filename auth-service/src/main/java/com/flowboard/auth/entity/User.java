package com.flowboard.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Represents all 5 actors in the FlowBoard system:
 *  - GUEST        : unauthenticated visitor (no DB row, handled via SecurityConfig)
 *  - MEMBER       : registered contributor
 *  - BOARD_ADMIN  : board-level admin / workspace admin
 *  - PLATFORM_ADMIN: system-wide administrator
 *  - SYSTEM       : internal automated account for scheduled jobs
 */
@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"),
    @UniqueConstraint(columnNames = "username")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String username;

    private String passwordHash;

    /**
     * Global platform role.
     * Board-level roles (OBSERVER / MEMBER / ADMIN) are stored in BoardMember.
     * Workspace-level roles (ADMIN / MEMBER) are stored in WorkspaceMember.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.MEMBER;

    @Column(length = 1024)
    private String avatarUrl;

    @Column(length = 1024)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    private String providerId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Enums ───────────────────────────────────────────────────────────────

    public enum Role {
        /** Registered contributor — can create workspaces, boards, cards */
        MEMBER,
        /** Workspace / board administrator — manages access and analytics */
        BOARD_ADMIN,
        /** Full system administrator */
        PLATFORM_ADMIN,
        /** Internal system account for scheduled jobs (due-date reminders, overdue detection) */
        SYSTEM
    }

    public enum AuthProvider {
        LOCAL, GOOGLE, GITHUB
    }

    // ── Convenience helpers ─────────────────────────────────────────────────

    public boolean isMember()        { return role == Role.MEMBER; }
    public boolean isBoardAdmin()    { return role == Role.BOARD_ADMIN || role == Role.PLATFORM_ADMIN; }
    public boolean isPlatformAdmin() { return role == Role.PLATFORM_ADMIN; }
    public boolean isSystemAccount() { return role == Role.SYSTEM; }
}
