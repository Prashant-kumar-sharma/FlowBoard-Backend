package com.flowboard.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "comment-service", contextId = "commentServiceDocsClient")
public interface CommentServiceDocsClient {

    @GetMapping("/v3/api-docs")
    String getApiDocs();
}
