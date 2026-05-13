package com.flowboard.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "label-service", contextId = "labelServiceDocsClient")
public interface LabelServiceDocsClient {

    @GetMapping("/v3/api-docs")
    String getApiDocs();
}
