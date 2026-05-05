package com.flowboard.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_otps", indexes = {
        @Index(name = "idx_auth_otp_email_purpose", columnList = "email,purpose")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Purpose purpose;

    @Column(nullable = false, length = 255)
    private String otpHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer attemptCount = 0;

    private LocalDateTime consumedAt;

    private String pendingFullName;

    private String pendingUsername;

    private String pendingPasswordHash;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum Purpose {
        REGISTRATION,
        LOGIN,
        RESET_PASSWORD
    }
}
