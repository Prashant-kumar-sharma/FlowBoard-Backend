package com.flowboard.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardCleanupClient {

    private final CardCleanupApiClient cardCleanupApiClient;

    public void deleteByBoardId(Long boardId) {
        cardCleanupApiClient.deleteByBoardId(boardId);
    }
}
