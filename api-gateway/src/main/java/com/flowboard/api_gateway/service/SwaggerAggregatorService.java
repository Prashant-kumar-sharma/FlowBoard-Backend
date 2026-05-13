package com.flowboard.api_gateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.api_gateway.client.AuthServiceDocsClient;
import com.flowboard.api_gateway.client.BoardServiceDocsClient;
import com.flowboard.api_gateway.client.CardServiceDocsClient;
import com.flowboard.api_gateway.client.CommentServiceDocsClient;
import com.flowboard.api_gateway.client.LabelServiceDocsClient;
import com.flowboard.api_gateway.client.ListServiceDocsClient;
import com.flowboard.api_gateway.client.NotificationServiceDocsClient;
import com.flowboard.api_gateway.client.WorkspaceServiceDocsClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
public class SwaggerAggregatorService {

    private final AuthServiceDocsClient authServiceDocsClient;
    private final WorkspaceServiceDocsClient workspaceServiceDocsClient;
    private final BoardServiceDocsClient boardServiceDocsClient;
    private final ListServiceDocsClient listServiceDocsClient;
    private final CardServiceDocsClient cardServiceDocsClient;
    private final CommentServiceDocsClient commentServiceDocsClient;
    private final LabelServiceDocsClient labelServiceDocsClient;
    private final NotificationServiceDocsClient notificationServiceDocsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SwaggerAggregatorService(AuthServiceDocsClient authServiceDocsClient,
                                    WorkspaceServiceDocsClient workspaceServiceDocsClient,
                                    BoardServiceDocsClient boardServiceDocsClient,
                                    ListServiceDocsClient listServiceDocsClient,
                                    CardServiceDocsClient cardServiceDocsClient,
                                    CommentServiceDocsClient commentServiceDocsClient,
                                    LabelServiceDocsClient labelServiceDocsClient,
                                    NotificationServiceDocsClient notificationServiceDocsClient) {
        this.authServiceDocsClient = authServiceDocsClient;
        this.workspaceServiceDocsClient = workspaceServiceDocsClient;
        this.boardServiceDocsClient = boardServiceDocsClient;
        this.listServiceDocsClient = listServiceDocsClient;
        this.cardServiceDocsClient = cardServiceDocsClient;
        this.commentServiceDocsClient = commentServiceDocsClient;
        this.labelServiceDocsClient = labelServiceDocsClient;
        this.notificationServiceDocsClient = notificationServiceDocsClient;
    }

    public Mono<Map<String, Object>> getAggregatedSwagger() {
        return Mono.fromCallable(() -> {
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

                    fetchServiceSwagger("auth-service", authServiceDocsClient::getApiDocs);
                    fetchServiceSwagger("workspace-service", workspaceServiceDocsClient::getApiDocs);
                    fetchServiceSwagger("board-service", boardServiceDocsClient::getApiDocs);
                    fetchServiceSwagger("list-service", listServiceDocsClient::getApiDocs);
                    fetchServiceSwagger("card-service", cardServiceDocsClient::getApiDocs);
                    fetchServiceSwagger("comment-service", commentServiceDocsClient::getApiDocs);
                    fetchServiceSwagger("label-service", labelServiceDocsClient::getApiDocs);
                    fetchServiceSwagger("notification-service", notificationServiceDocsClient::getApiDocs);

                    return aggregatedSwagger;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private void fetchServiceSwagger(String serviceName, Supplier<String> responseSupplier) {
        try {
            String response = responseSupplier.get();
            JsonNode swaggerDoc = objectMapper.readTree(response);
            JsonNode infoNode = swaggerDoc.get("info");
            String title = infoNode != null && infoNode.get("title") != null
                    ? infoNode.get("title").asText()
                    : serviceName;
            log.info("Fetched Swagger for {}: {}", serviceName, title);
        } catch (FeignException ex) {
            log.warn("Unable to fetch Swagger for {}: {}", serviceName, ex.getMessage());
        } catch (Exception ex) {
            log.error("Error parsing Swagger for {}: {}", serviceName, ex.getMessage(), ex);
        }
    }
}
