package com.flowboard.list.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CardCleanupClientTest {

    @Test
    void deleteByListIdCallsCleanupEndpoint() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        CardCleanupClient client = new CardCleanupClient(restClient);

        server.expect(requestTo("http://localhost/list/11"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.deleteByListId(11L);

        server.verify();
    }
}
