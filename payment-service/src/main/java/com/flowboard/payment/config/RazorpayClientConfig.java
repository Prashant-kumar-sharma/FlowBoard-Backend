package com.flowboard.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class RazorpayClientConfig {

    @Bean
    RestClient razorpayRestClient(RestClient.Builder builder,
                                  @Value("${payment.razorpay.api-base-url}") String apiBaseUrl,
                                  @Value("${payment.razorpay.key-id}") String keyId,
                                  @Value("${payment.razorpay.key-secret}") String keySecret) {
        String credentials = Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

        return builder.baseUrl(apiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}
