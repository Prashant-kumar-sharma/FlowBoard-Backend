package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.response.PaymentEntitlementResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "workspacePaymentEntitlementApiClient", url = "${payment.service.base-url}")
public interface PaymentEntitlementApiClient {

    @GetMapping("/users/{userId}/entitlement")
    PaymentEntitlementResponse getEntitlement(@PathVariable Long userId);
}
