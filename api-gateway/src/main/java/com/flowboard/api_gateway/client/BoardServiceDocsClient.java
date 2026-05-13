package com.flowboard.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "board-service", contextId = "boardServiceDocsClient")
public interface BoardServiceDocsClient {

    @GetMapping("/v3/api-docs")
    String getApiDocs();
}
