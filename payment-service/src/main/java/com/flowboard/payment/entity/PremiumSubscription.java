package com.flowboard.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "premium_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String planCode;

    @Column(nullable = false, length = 16)
    private String providerName;

    @Column(length = 64)
    private String providerOrderId;

    @Column(length = 64)
    private String providerPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    private LocalDateTime activatedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
