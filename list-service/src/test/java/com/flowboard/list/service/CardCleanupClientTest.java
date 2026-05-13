package com.flowboard.list.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CardCleanupClientTest {

    @Test
    void deleteByListIdDelegatesToFeignClient() {
        CardCleanupApiClient apiClient = mock(CardCleanupApiClient.class);
        CardCleanupClient client = new CardCleanupClient(apiClient);

        client.deleteByListId(11L);

        verify(apiClient).deleteByListId(11L);
    }
}
