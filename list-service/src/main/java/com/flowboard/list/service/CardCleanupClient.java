package com.flowboard.list.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CardCleanupClient {

    private final RestClient cardCleanupRestClient;

    public void deleteByListId(Long listId) {
        cardCleanupRestClient.delete()
                .uri("/list/{listId}", listId)
                .retrieve()
                .toBodilessEntity();
    }
}
