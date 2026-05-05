package com.flowboard.payment.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String PREMIUM_ACTIVATED_TOPIC = "flowboard.payment.premium.activated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPremiumActivated(PremiumActivatedEvent event) {
        Map<String, Object> payload = Map.of(
                "userId", event.userId(),
                "planCode", event.planCode(),
                "planName", event.planName(),
                "amountPaise", event.amountPaise(),
                "currency", event.currency(),
                "providerName", event.providerName(),
                "providerOrderId", event.providerOrderId(),
                "providerPaymentId", event.providerPaymentId(),
                "activatedAt", event.activatedAt().toString()
        );

        kafkaTemplate.send(PREMIUM_ACTIVATED_TOPIC, String.valueOf(event.userId()), payload);
        log.info("Premium activated event sent for userId={} orderId={}", event.userId(), event.providerOrderId());
    }

    public record PremiumActivatedEvent(Long userId,
                                        String planCode,
                                        String planName,
                                        Integer amountPaise,
                                        String currency,
                                        String providerName,
                                        String providerOrderId,
                                        String providerPaymentId,
                                        LocalDateTime activatedAt) {
    }
}
