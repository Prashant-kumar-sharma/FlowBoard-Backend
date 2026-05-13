package com.flowboard.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListCleanupClient {

    private final ListCleanupApiClient listCleanupApiClient;

    public void deleteByBoardId(Long boardId) {
        listCleanupApiClient.deleteByBoardId(boardId);
    }
}
