package com.flowboard.list.controller;

import com.flowboard.list.dto.request.CreateListRequest;
import com.flowboard.list.dto.response.ListResponse;
import com.flowboard.list.service.ListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j @RestController @RequestMapping("/api/v1/lists")
@RequiredArgsConstructor @Tag(name = "Lists") @SecurityRequirement(name = "bearerAuth")
public class ListController {
    private final ListService listService;
    private final com.flowboard.list.repository.TaskListRepository taskListRepository;

    // TEMPORARY DEBUG ENDPOINT - remove after fixing
    @GetMapping("/debug/all")
    @Operation(summary = "[DEBUG] Get ALL lists in DB with their boardId")
    public ResponseEntity<List<Map<String, Object>>> debugAll() {
        var allLists = taskListRepository.findAll();
        var result = allLists.stream().map(l -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", l.getId());
            map.put("name", l.getName());
            map.put("boardId", l.getBoardId());
            map.put("position", l.getPosition());
            map.put("isArchived", l.getIsArchived());
            return map;
        }).toList();
        log.info(">>> DEBUG ALL LISTS: {}", result);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Create a list")
    public ResponseEntity<ListResponse> create(@RequestBody CreateListRequest request,
                                            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listService.create(request, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get list by ID")
    public ResponseEntity<ListResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(listService.getById(id));
    }

    @GetMapping("/board/{boardId}")
    @Operation(summary = "Get all lists in a board")
    public ResponseEntity<List<ListResponse>> getByBoard(
            @PathVariable Long boardId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(listService.getByBoard(boardId, userId));
    }

    @GetMapping("/board/{boardId}/archived")
    @Operation(summary = "Get archived lists in a board")
    public ResponseEntity<List<ListResponse>> getArchived(
            @PathVariable Long boardId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(listService.getArchivedByBoard(boardId, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update list")
    public ResponseEntity<ListResponse> update(@PathVariable Long id, @RequestBody CreateListRequest request,
                                            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(listService.update(id, request, userId));
    }

    @PutMapping("/board/{boardId}/reorder")
    @Operation(summary = "Reorder lists in a board")
    public ResponseEntity<Void> reorder(@PathVariable Long boardId, @RequestBody List<Long> orderedIds,
                                         @RequestHeader("X-User-Id") Long userId) {
        listService.reorder(boardId, orderedIds, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/archive")
    @Operation(summary = "Archive list")
    public ResponseEntity<Void> archive(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        listService.archive(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unarchive")
    @Operation(summary = "Unarchive list")
    public ResponseEntity<Void> unarchive(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        listService.unarchive(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/move")
    @Operation(summary = "Move list to another board")
    public ResponseEntity<ListResponse> move(@PathVariable Long id, @RequestBody com.flowboard.list.dto.request.MoveListRequest request,
                                          @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(listService.move(id, request.getTargetBoardId(), userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete list")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        listService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal/board/{boardId}")
    @Operation(summary = "Internal cleanup - delete all lists by board")
    public ResponseEntity<Void> deleteByBoardInternal(@PathVariable Long boardId) {
        listService.deleteByBoardId(boardId);
        return ResponseEntity.noContent().build();
    }
}
