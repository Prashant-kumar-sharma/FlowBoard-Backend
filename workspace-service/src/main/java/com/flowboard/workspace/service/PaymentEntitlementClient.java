package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.response.PaymentEntitlementResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PaymentEntitlementClient {

    private final RestClient paymentRestClient;

    public PaymentEntitlementClient(@Qualifier("paymentRestClient") RestClient paymentRestClient) {
        this.paymentRestClient = paymentRestClient;
    }
    public boolean isPremium(Long userId) {
        try {
            PaymentEntitlementResponse response = paymentRestClient.get()
                    .uri("/users/{userId}/entitlement", userId)
                    .retrieve()
                    .body(PaymentEntitlementResponse.class);
            return response != null && response.isPremium();
        } catch (Exception ex) {
            log.warn("Falling back to free tier for user {} because payment entitlement lookup failed: {}", userId, ex.getMessage());
            return false;
        }
    }
}
