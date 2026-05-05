package com.flowboard.workspace.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BoardCleanupClientTest {

    @Test
    void deleteByWorkspaceIdCallsCleanupEndpointWithActorQueryParameter() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        BoardCleanupClient client = new BoardCleanupClient(restClient);

        server.expect(requestTo("http://localhost/workspace/13?actorId=99"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.deleteByWorkspaceId(13L, 99L);

        server.verify();
    }
}
