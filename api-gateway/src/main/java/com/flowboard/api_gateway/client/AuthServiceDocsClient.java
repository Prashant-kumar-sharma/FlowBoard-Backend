package com.flowboard.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "auth-service", contextId = "authServiceDocsClient")
public interface AuthServiceDocsClient {

    @GetMapping("/v3/api-docs")
    String getApiDocs();
}
