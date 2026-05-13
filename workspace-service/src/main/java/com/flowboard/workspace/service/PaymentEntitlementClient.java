package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.response.PaymentEntitlementResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentEntitlementClient {

    private final PaymentEntitlementApiClient paymentEntitlementApiClient;

    public PaymentEntitlementClient(PaymentEntitlementApiClient paymentEntitlementApiClient) {
        this.paymentEntitlementApiClient = paymentEntitlementApiClient;
    }

    public boolean isPremium(Long userId) {
        try {
            PaymentEntitlementResponse response = paymentEntitlementApiClient.getEntitlement(userId);
            return response != null && response.isPremium();
        } catch (Exception ex) {
            log.warn("Falling back to free tier for user {} because payment entitlement lookup failed: {}", userId, ex.getMessage());
            return false;
        }
    }
}
