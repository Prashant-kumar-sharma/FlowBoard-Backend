package com.flowboard.api_gateway.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class SwaggerAggregatorService {

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<Map<String, Object>> getAggregatedSwagger() {
        Map<String, Object> aggregatedSwagger = new HashMap<>();
        aggregatedSwagger.put("openapi", "3.0.1");
        aggregatedSwagger.put("info", Map.of(
            "title", "FlowBoard API Gateway",
            "description", "Aggregated API documentation for all microservices",
            "version", "1.0.0"
        ));
        aggregatedSwagger.put("servers", new Object[]{Map.of("url", "http://localhost:8080")});
        aggregatedSwagger.put("paths", new HashMap<>());
        aggregatedSwagger.put("components", Map.of("schemas", new HashMap<>()));

        return fetchServiceSwagger("auth-service")
            .then(fetchServiceSwagger("workspace-service"))
            .then(fetchServiceSwagger("board-service"))
            .then(fetchServiceSwagger("list-service"))
            .then(fetchServiceSwagger("card-service"))
            .then(fetchServiceSwagger("comment-service"))
            .then(fetchServiceSwagger("label-service"))
            .then(fetchServiceSwagger("notification-service"))
            .then(Mono.just(aggregatedSwagger));
    }

    private Mono<Void> fetchServiceSwagger(String serviceName) {
        return Mono.fromCallable(() -> discoveryClient.getInstances(serviceName))
            .filter(instances -> !instances.isEmpty())
            .flatMap(instances -> {
                ServiceInstance instance = instances.get(0);
                String baseUrl = instance.getUri().toString();
                String swaggerUrl = baseUrl + "/v3/api-docs";
                
                return webClient.get()
                    .uri(swaggerUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(response -> {
                        try {
                            JsonNode swaggerDoc = objectMapper.readTree(response);
                            // Here you would merge the swagger documentation
                            // For now, we'll just log it
                            System.out.println("Fetched Swagger for " + serviceName + ": " + swaggerDoc.get("info").get("title").asText());
                        } catch (Exception e) {
                            System.err.println("Error parsing Swagger for " + serviceName + ": " + e.getMessage());
                        }
                    })
                    .then();
            })
            .then();
    }
}
