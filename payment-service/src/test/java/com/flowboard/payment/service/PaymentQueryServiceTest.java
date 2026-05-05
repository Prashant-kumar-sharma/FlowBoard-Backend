package com.flowboard.payment.service;

import com.flowboard.payment.dto.response.PaymentEntitlementResponse;
import com.flowboard.payment.dto.response.PaymentSummaryResponse;
import com.flowboard.payment.entity.PremiumSubscription;
import com.flowboard.payment.repository.PremiumSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentQueryServiceTest {

    @Mock
    private PremiumSubscriptionRepository premiumSubscriptionRepository;

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentQueryService, "razorpayKeyId", "rzp_test");
        ReflectionTestUtils.setField(paymentQueryService, "razorpayKeySecret", "secret-123");
        ReflectionTestUtils.setField(paymentQueryService, "premiumAmountPaise", 49900);
        ReflectionTestUtils.setField(paymentQueryService, "currency", "INR");
        ReflectionTestUtils.setField(paymentQueryService, "freeWorkspaceLimit", 5);
        ReflectionTestUtils.setField(paymentQueryService, "freeMemberLimit", 5);
    }

    @Test
    void getSummaryReturnsPremiumStateWhenSubscriptionActive() {
        PremiumSubscription subscription = PremiumSubscription.builder()
                .userId(1L)
                .status(PremiumSubscription.Status.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();
        when(premiumSubscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(subscription));

        PaymentSummaryResponse summary = paymentQueryService.getSummary(1L);

        assertThat(summary.isPremium()).isTrue();
        assertThat(summary.getPlanName()).isEqualTo("FlowBoard Premium");
        assertThat(summary.getRazorpayKeyId()).isEqualTo("rzp_test");
    }

    @Test
    void getEntitlementReturnsFreeLimitsForNonPremiumUser() {
        when(premiumSubscriptionRepository.findByUserId(1L)).thenReturn(Optional.empty());

        PaymentEntitlementResponse response = paymentQueryService.getEntitlement(1L);

        assertThat(response.isPremium()).isFalse();
        assertThat(response.getWorkspaceLimit()).isEqualTo(5);
        assertThat(response.getMemberLimit()).isEqualTo(5);
    }

    @Test
    void isPremiumUserReturnsFalseForInactiveSubscription() {
        PremiumSubscription subscription = PremiumSubscription.builder()
                .status(PremiumSubscription.Status.INACTIVE)
                .build();
        when(premiumSubscriptionRepository.findByUserId(2L)).thenReturn(Optional.of(subscription));

        assertThat(paymentQueryService.isPremiumUser(2L)).isFalse();
    }

    @Test
    void verifySignatureReturnsTrueForMatchingSignature() throws Exception {
        String orderId = "order_123";
        String paymentId = "pay_123";
        String payload = orderId + "|" + paymentId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret-123".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String signature = bytesToHex(hash);

        assertThat(paymentQueryService.verifySignature(orderId, paymentId, signature)).isTrue();
    }

    @Test
    void verifySignatureReturnsFalseForBlankSignature() {
        assertThat(paymentQueryService.verifySignature("order", "pay", " ")).isFalse();
    }

    @Test
    void verifySignatureWrapsUnexpectedCryptoErrors() {
        ReflectionTestUtils.setField(paymentQueryService, "razorpayKeySecret", null);

        assertThatThrownBy(() -> paymentQueryService.verifySignature("order", "pay", "sig"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to verify");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
