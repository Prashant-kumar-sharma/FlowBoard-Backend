package com.flowboard.list.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardCleanupClient {

    private final CardCleanupApiClient cardCleanupApiClient;

    public void deleteByListId(Long listId) {
        cardCleanupApiClient.deleteByListId(listId);
    }
}
