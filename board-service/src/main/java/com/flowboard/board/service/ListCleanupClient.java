package com.flowboard.board.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ListCleanupClient {

    private final RestClient listCleanupRestClient;

    public ListCleanupClient(@Qualifier("listCleanupRestClient") RestClient listCleanupRestClient) {
        this.listCleanupRestClient = listCleanupRestClient;
    }

    public void deleteByBoardId(Long boardId) {
        listCleanupRestClient.delete()
                .uri("/board/{boardId}", boardId)
                .retrieve()
                .toBodilessEntity();
    }
}
