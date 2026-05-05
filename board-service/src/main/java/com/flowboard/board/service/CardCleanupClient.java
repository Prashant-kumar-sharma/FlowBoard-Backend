package com.flowboard.board.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CardCleanupClient {

    private final RestClient cardCleanupRestClient;

    public CardCleanupClient(@Qualifier("cardCleanupRestClient") RestClient cardCleanupRestClient) {
        this.cardCleanupRestClient = cardCleanupRestClient;
    }

    public void deleteByBoardId(Long boardId) {
        cardCleanupRestClient.delete()
                .uri("/board/{boardId}", boardId)
                .retrieve()
                .toBodilessEntity();
    }
}
