package com.flowboard.board.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ListCleanupClientTest {

    @Test
    void deleteByBoardIdDelegatesToFeignClient() {
        ListCleanupApiClient apiClient = mock(ListCleanupApiClient.class);
        ListCleanupClient client = new ListCleanupClient(apiClient);

        client.deleteByBoardId(9L);

        verify(apiClient).deleteByBoardId(9L);
    }
}
