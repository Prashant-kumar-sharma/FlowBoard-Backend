package com.flowboard.board.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "boardCardCleanupApiClient", url = "${card.service.base-url}")
public interface CardCleanupApiClient {

    @DeleteMapping("/board/{boardId}")
    void deleteByBoardId(@PathVariable Long boardId);
}
