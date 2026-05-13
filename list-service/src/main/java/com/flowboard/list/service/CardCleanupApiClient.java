package com.flowboard.list.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "listCardCleanupApiClient", url = "${card.service.base-url}")
public interface CardCleanupApiClient {

    @DeleteMapping("/list/{listId}")
    void deleteByListId(@PathVariable Long listId);
}
