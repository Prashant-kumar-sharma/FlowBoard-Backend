package com.flowboard.api_gateway.controller;

import com.flowboard.api_gateway.service.SwaggerAggregatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/v3/api-docs")
public class SwaggerController {

    private final SwaggerAggregatorService swaggerAggregatorService;

    public SwaggerController(SwaggerAggregatorService swaggerAggregatorService) {
        this.swaggerAggregatorService = swaggerAggregatorService;
    }

    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> getAggregatedSwagger() {
        return swaggerAggregatorService.getAggregatedSwagger()
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
