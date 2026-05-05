package com.flowboard.board.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CleanupClientConfig {

    @Bean
    RestClient listCleanupRestClient(
            RestClient.Builder builder,
            @Value("${list.service.base-url}") String listServiceBaseUrl) {
        return builder.baseUrl(listServiceBaseUrl).build();
    }

    @Bean
    RestClient cardCleanupRestClient(
            RestClient.Builder builder,
            @Value("${card.service.base-url}") String cardServiceBaseUrl) {
        return builder.baseUrl(cardServiceBaseUrl).build();
    }
}
