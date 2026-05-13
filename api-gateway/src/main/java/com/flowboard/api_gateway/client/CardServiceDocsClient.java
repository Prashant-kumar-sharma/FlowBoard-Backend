package com.flowboard.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "card-service", contextId = "cardServiceDocsClient")
public interface CardServiceDocsClient {

    @GetMapping("/v3/api-docs")
    String getApiDocs();
}
