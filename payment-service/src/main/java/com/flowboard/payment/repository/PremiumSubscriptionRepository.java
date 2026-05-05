package com.flowboard.payment.repository;

import com.flowboard.payment.entity.PremiumSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PremiumSubscriptionRepository extends JpaRepository<PremiumSubscription, Long> {
    Optional<PremiumSubscription> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
