package com.flowboard.workspace.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "workspaceBoardCleanupApiClient", url = "${board.service.base-url}")
public interface BoardCleanupApiClient {

    @DeleteMapping("/workspace/{workspaceId}")
    void deleteByWorkspaceId(@PathVariable Long workspaceId, @RequestParam Long actorId);
}
