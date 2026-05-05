package com.flowboard.workspace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BoardCleanupClientConfig {

    @Bean
    RestClient boardCleanupRestClient(
            RestClient.Builder builder,
            @Value("${board.service.base-url}") String boardServiceBaseUrl) {
        return builder.baseUrl(boardServiceBaseUrl).build();
    }
}
