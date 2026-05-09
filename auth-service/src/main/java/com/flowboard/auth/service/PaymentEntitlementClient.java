package com.flowboard.auth.service;

import com.flowboard.auth.dto.response.PaymentEntitlementResponse;
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

    public PaymentEntitlementResponse getEntitlement(Long userId) {
        try {
            return paymentRestClient.get()
                    .uri("/users/{userId}/entitlement", userId)
                    .retrieve()
                    .body(PaymentEntitlementResponse.class);
        } catch (Exception ex) {
            log.warn("Unable to fetch payment entitlement for user {}: {}", userId, ex.getMessage());
            PaymentEntitlementResponse fallback = new PaymentEntitlementResponse();
            fallback.setUserId(userId);
            fallback.setPremium(false);
            fallback.setPlanCode("FREE");
            return fallback;
        }
    }
}
