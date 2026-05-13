package com.flowboard.auth.service;

import com.flowboard.auth.dto.response.PaymentEntitlementResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEntitlementClient {

    private final PaymentEntitlementApiClient paymentEntitlementApiClient;

    public PaymentEntitlementClient(PaymentEntitlementApiClient paymentEntitlementApiClient) {
        this.paymentEntitlementApiClient = paymentEntitlementApiClient;
    }

    public PaymentEntitlementResponse getEntitlement(Long userId) {
        try {
            return paymentEntitlementApiClient.getEntitlement(userId);
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
