package com.flowboard.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentCleanupClient {

    private final PaymentCleanupApiClient paymentCleanupApiClient;

    public PaymentCleanupClient(PaymentCleanupApiClient paymentCleanupApiClient) {
        this.paymentCleanupApiClient = paymentCleanupApiClient;
    }

    public void deleteUserPaymentData(Long userId) {
        try {
            paymentCleanupApiClient.deleteUserPaymentData(userId);
        } catch (Exception ex) {
            log.warn("Unable to delete payment data for user {}: {}", userId, ex.getMessage());
        }
    }
}
