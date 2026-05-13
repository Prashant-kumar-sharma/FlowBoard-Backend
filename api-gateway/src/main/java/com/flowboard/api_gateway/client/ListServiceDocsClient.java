package com.flowboard.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "list-service", contextId = "listServiceDocsClient")
public interface ListServiceDocsClient {

    @GetMapping("/v3/api-docs")
    String getApiDocs();
}
