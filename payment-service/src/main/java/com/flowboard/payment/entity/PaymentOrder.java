package com.flowboard.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 64)
    private String providerOrderId;

    @Column(length = 64)
    private String providerPaymentId;

    @Column(nullable = false, length = 32)
    private String providerName;

    @Column(nullable = false, length = 64)
    private String planCode;

    @Column(nullable = false)
    private Integer amountPaise;

    @Column(nullable = false, length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(length = 512)
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Status {
        CREATED,
        PAID,
        FAILED
    }
}
