package com.flowboard.workspace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardCleanupClient {

    private final BoardCleanupApiClient boardCleanupApiClient;

    public void deleteByWorkspaceId(Long workspaceId, Long actorId) {
        boardCleanupApiClient.deleteByWorkspaceId(workspaceId, actorId);
    }
}
