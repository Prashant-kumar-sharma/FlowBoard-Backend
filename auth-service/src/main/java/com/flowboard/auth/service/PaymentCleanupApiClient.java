package com.flowboard.auth.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "authPaymentCleanupApiClient", url = "${payment.service.base-url}")
public interface PaymentCleanupApiClient {

    @DeleteMapping("/users/{userId}")
    void deleteUserPaymentData(@PathVariable Long userId);
}
