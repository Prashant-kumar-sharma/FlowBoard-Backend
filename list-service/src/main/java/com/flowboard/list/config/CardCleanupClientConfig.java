package com.flowboard.list.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CardCleanupClientConfig {

    @Bean
    RestClient cardCleanupRestClient(
            RestClient.Builder builder,
            @Value("${card.service.base-url}") String cardServiceBaseUrl) {
        return builder.baseUrl(cardServiceBaseUrl).build();
    }
}
