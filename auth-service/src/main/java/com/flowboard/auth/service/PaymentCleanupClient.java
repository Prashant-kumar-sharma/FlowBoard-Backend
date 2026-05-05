package com.flowboard.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PaymentCleanupClient {

    private final RestClient paymentRestClient;

    public PaymentCleanupClient(@Qualifier("paymentRestClient") RestClient paymentRestClient) {
        this.paymentRestClient = paymentRestClient;
    }

    public void deleteUserPaymentData(Long userId) {
        try {
            paymentRestClient.delete()
                    .uri("/users/{userId}", userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Unable to delete payment data for user {}: {}", userId, ex.getMessage());
        }
    }
}
