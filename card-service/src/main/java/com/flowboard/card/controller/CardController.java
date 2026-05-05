package com.flowboard.card.controller;

import com.flowboard.card.dto.request.*;
import com.flowboard.card.dto.response.CardResponse;
import com.flowboard.card.entity.Card;
import com.flowboard.card.entity.CardActivity;
import com.flowboard.card.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j @RestController @RequestMapping("/api/v1/cards")
@RequiredArgsConstructor @Tag(name = "Cards") @SecurityRequirement(name = "bearerAuth")
public class CardController {
    private final CardService cardService;

    @PostMapping
    @Operation(summary = "Create a card")
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CreateCardRequest req,
                                                @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.create(req, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(cardService.getById(id, userId));
    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<List<CardResponse>> getByList(
            @PathVariable Long listId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(cardService.getByList(listId, userId));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<CardResponse>> getByBoard(
            @PathVariable Long boardId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(cardService.getByBoard(boardId, userId));
    }

    @GetMapping("/assignee/{userId}")
    public ResponseEntity<List<CardResponse>> getByAssignee(@PathVariable Long userId) {
        return ResponseEntity.ok(cardService.getByAssignee(userId));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<CardResponse>> getOverdue() {
        return ResponseEntity.ok(cardService.getOverdue());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<CardResponse>> getAllForAdmin(@RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        return ResponseEntity.ok(cardService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> update(@PathVariable Long id, @RequestBody CreateCardRequest req,
                                                @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cardService.update(id, req, userId));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<CardResponse> move(@PathVariable Long id, @RequestBody MoveCardRequest req,
                                              @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cardService.move(id, req.getTargetListId(), req.getPosition(), userId));
    }

    @PutMapping("/list/{listId}/reorder")
    public ResponseEntity<Void> reorder(@PathVariable Long listId, @RequestBody List<Long> orderedIds,
                                         @RequestHeader("X-User-Id") Long userId) {
        cardService.reorder(listId, orderedIds, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        cardService.archive(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<Void> unarchive(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        cardService.unarchive(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assignee")
    public ResponseEntity<CardResponse> setAssignee(@PathVariable Long id, @RequestBody AssignCardRequest req,
                                                     @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cardService.setAssignee(id, req.getAssigneeId(), userId));
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<CardResponse> setPriority(@PathVariable Long id, @RequestBody UpdatePriorityRequest req,
                                                     @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cardService.setPriority(id, Card.Priority.valueOf(req.getPriority()), userId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CardResponse> setStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest req,
                                                   @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(cardService.setStatus(id, Card.Status.valueOf(req.getStatus()), userId));
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<List<CardActivity>> getActivity(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(cardService.getActivity(id, userId));
    }

    @GetMapping("/admin/activity")
    public ResponseEntity<List<CardActivity>> getAllActivity(@RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        return ResponseEntity.ok(cardService.getAllActivity());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        cardService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal/board/{boardId}")
    public ResponseEntity<Void> deleteByBoardInternal(@PathVariable Long boardId) {
        cardService.deleteByBoardId(boardId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal/list/{listId}")
    public ResponseEntity<Void> deleteByListInternal(@PathVariable Long listId) {
        cardService.deleteByListId(listId);
        return ResponseEntity.noContent().build();
    }

    private void assertPlatformAdmin(String requesterRole) {
        if (!"PLATFORM_ADMIN".equalsIgnoreCase(requesterRole)) {
            throw new IllegalArgumentException("Platform admin access required");
        }
    }
}
