package com.flowboard.auth.service;

import com.flowboard.auth.dto.response.PaymentEntitlementResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "authPaymentEntitlementApiClient", url = "${payment.service.base-url}")
public interface PaymentEntitlementApiClient {

    @GetMapping("/users/{userId}/entitlement")
    PaymentEntitlementResponse getEntitlement(@PathVariable Long userId);
}
