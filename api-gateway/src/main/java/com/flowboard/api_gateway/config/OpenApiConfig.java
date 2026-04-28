package com.flowboard.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * OpenAPI Configuration for API Gateway.
 * This configuration enables Swagger UI to work properly with Spring Cloud Gateway.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure OpenAPI documentation for the API Gateway.
     * This provides metadata about the API that will be displayed in Swagger UI.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FlowBoard API Gateway")
                        .version("1.0")
                        .description("API Gateway for FlowBoard Application - Provides unified access to all microservices")
                        .contact(new Contact()
                                .name("FlowBoard Team")
                                .email("support@flowboard.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server")
                ));
    }
}
