package com.flowboard.board.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CardCleanupClientTest {

    @Test
    void deleteByBoardIdDelegatesToFeignClient() {
        CardCleanupApiClient apiClient = mock(CardCleanupApiClient.class);
        CardCleanupClient client = new CardCleanupClient(apiClient);

        client.deleteByBoardId(7L);

        verify(apiClient).deleteByBoardId(7L);
    }
}
