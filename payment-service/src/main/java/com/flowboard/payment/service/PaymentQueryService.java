package com.flowboard.payment.service;

import com.flowboard.payment.dto.response.PaymentEntitlementResponse;
import com.flowboard.payment.dto.response.PaymentSummaryResponse;
import com.flowboard.payment.entity.PremiumSubscription;
import com.flowboard.payment.repository.PremiumSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private static final String PREMIUM_PLAN_CODE = "PREMIUM_MONTHLY";
    private static final String PREMIUM_PLAN_NAME = "FlowBoard Premium";

    private final PremiumSubscriptionRepository premiumSubscriptionRepository;

    @Value("${payment.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${payment.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${payment.plan.premium-amount-paise}")
    private Integer premiumAmountPaise;

    @Value("${payment.plan.currency}")
    private String currency;

    @Value("${payment.plan.workspace-limit}")
    private Integer freeWorkspaceLimit;

    @Value("${payment.plan.member-limit}")
    private Integer freeMemberLimit;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "payment:summary", key = "#userId")
    public PaymentSummaryResponse getSummary(Long userId) {
        PremiumSubscription subscription = premiumSubscriptionRepository.findByUserId(userId).orElse(null);
        boolean premium = isPremium(subscription);

        return PaymentSummaryResponse.builder()
                .userId(userId)
                .premium(premium)
                .planCode(premium ? PREMIUM_PLAN_CODE : "FREE")
                .planName(premium ? PREMIUM_PLAN_NAME : "FlowBoard Free")
                .workspaceLimit(premium ? null : freeWorkspaceLimit)
                .memberLimit(premium ? null : freeMemberLimit)
                .premiumAmountPaise(premiumAmountPaise)
                .currency(currency)
                .razorpayKeyId(razorpayKeyId)
                .activatedAt(subscription != null ? subscription.getActivatedAt() : null)
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "payment:entitlement", key = "#userId")
    public PaymentEntitlementResponse getEntitlement(Long userId) {
        boolean premium = isPremiumUser(userId);
        return PaymentEntitlementResponse.builder()
                .userId(userId)
                .premium(premium)
                .workspaceLimit(premium ? null : freeWorkspaceLimit)
                .memberLimit(premium ? null : freeMemberLimit)
                .planCode(premium ? PREMIUM_PLAN_CODE : "FREE")
                .build();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "payment:premium", key = "#userId")
    public boolean isPremiumUser(Long userId) {
        return isPremium(premiumSubscriptionRepository.findByUserId(userId).orElse(null));
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public boolean verifySignature(String orderId, String paymentId, String razorpaySignature) {
        if (razorpaySignature == null || razorpaySignature.isBlank()) {
            return false;
        }
        try {
            String payload = orderId + "|" + paymentId;
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return toHex(hash).equals(razorpaySignature);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to verify Razorpay signature", ex);
        }
    }

    private boolean isPremium(PremiumSubscription subscription) {
        return subscription != null && subscription.getStatus() == PremiumSubscription.Status.ACTIVE;
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
