package com.flowboard.workspace.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BoardCleanupClientTest {

    @Test
    void deleteByWorkspaceIdDelegatesToFeignClient() {
        BoardCleanupApiClient apiClient = mock(BoardCleanupApiClient.class);
        BoardCleanupClient client = new BoardCleanupClient(apiClient);

        client.deleteByWorkspaceId(13L, 99L);

        verify(apiClient).deleteByWorkspaceId(13L, 99L);
    }
}
