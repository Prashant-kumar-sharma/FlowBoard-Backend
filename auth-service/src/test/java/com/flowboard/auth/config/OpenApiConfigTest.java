package com.flowboard.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void flowBoardOpenApiExposesExpectedMetadataAndSecurityScheme() {
        OpenAPI openAPI = new OpenApiConfig().flowBoardOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("FlowBoard API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1.0.0");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getComponents().getSecuritySchemes().get("bearerAuth").getScheme()).isEqualTo("bearer");
    }
}
