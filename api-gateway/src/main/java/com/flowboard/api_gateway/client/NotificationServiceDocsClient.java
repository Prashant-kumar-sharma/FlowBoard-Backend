package com.flowboard.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "notification-service", contextId = "notificationServiceDocsClient")
public interface NotificationServiceDocsClient {

    @GetMapping("/v3/api-docs")
    String getApiDocs();
}
