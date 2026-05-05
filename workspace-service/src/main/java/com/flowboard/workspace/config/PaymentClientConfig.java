package com.flowboard.workspace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PaymentClientConfig {

    @Bean
    RestClient paymentRestClient(RestClient.Builder builder,
                                 @Value("${payment.service.base-url}") String paymentServiceBaseUrl) {
        return builder.baseUrl(paymentServiceBaseUrl).build();
    }
}
