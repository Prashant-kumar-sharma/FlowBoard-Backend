package com.flowboard.workspace.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BoardCleanupClient {

    private final RestClient boardCleanupRestClient;

    public BoardCleanupClient(@Qualifier("boardCleanupRestClient") RestClient boardCleanupRestClient) {
        this.boardCleanupRestClient = boardCleanupRestClient;
    }

    public void deleteByWorkspaceId(Long workspaceId, Long actorId) {
        boardCleanupRestClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/workspace/{workspaceId}")
                        .queryParam("actorId", actorId)
                        .build(workspaceId))
                .retrieve()
                .toBodilessEntity();
    }
}
