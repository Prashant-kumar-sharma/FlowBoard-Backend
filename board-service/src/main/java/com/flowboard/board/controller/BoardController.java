package com.flowboard.board.controller;

import com.flowboard.board.dto.request.CreateBoardRequest;
import com.flowboard.board.dto.response.BoardMemberResponse;
import com.flowboard.board.dto.response.BoardResponse;
import com.flowboard.board.service.BoardService;
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

@Slf4j @RestController @RequestMapping("/api/v1/boards")
@RequiredArgsConstructor @Tag(name = "Boards") @SecurityRequirement(name = "bearerAuth")
public class BoardController {

    private final BoardService boardService;

    @PostMapping
    @Operation(summary = "Create a board")
    public ResponseEntity<BoardResponse> create(@Valid @RequestBody CreateBoardRequest req,
                                                 @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.create(req, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get board by ID")
    public ResponseEntity<BoardResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(boardService.getById(id, userId));
    }

    @GetMapping("/workspace/{workspaceId}")
    @Operation(summary = "Get boards by workspace ID")
    public ResponseEntity<List<BoardResponse>> getByWorkspace(
            @PathVariable Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(boardService.getByWorkspace(workspaceId, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BoardResponse>> getMy(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(boardService.getByMember(userId));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<BoardResponse>> getAllForAdmin(@RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        return ResponseEntity.ok(boardService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoardResponse> update(@PathVariable Long id,
                                                 @RequestBody CreateBoardRequest req,
                                                 @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(boardService.update(id, req, userId));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> close(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        boardService.closeBoard(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        boardService.deleteBoard(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/{id}/close")
    public ResponseEntity<Void> adminClose(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        boardService.adminCloseBoard(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> adminDelete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        boardService.adminDeleteBoard(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal/workspace/{workspaceId}")
    public ResponseEntity<Void> deleteByWorkspaceInternal(
            @PathVariable Long workspaceId,
            @RequestParam Long actorId) {
        boardService.deleteByWorkspaceId(workspaceId, actorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<BoardMemberResponse> addMember(@PathVariable Long id,
                                                          @RequestBody com.flowboard.board.dto.request.AddBoardMemberRequest req,
                                                          @RequestHeader("X-User-Id") Long requesterId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.addMember(id, req.getUserId(), req.getRole(), requesterId));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId,
                                              @RequestHeader("X-User-Id") Long requesterId) {
        boardService.removeMember(id, userId, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<BoardMemberResponse> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody com.flowboard.board.dto.request.UpdateBoardMemberRoleRequest req,
            @RequestHeader("X-User-Id") Long requesterId) {
        return ResponseEntity.ok(boardService.updateMemberRole(id, userId, req.getRole(), requesterId));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<BoardMemberResponse>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getMembers(id));
    }

    @GetMapping("/admin/audit")
    public ResponseEntity<List<com.flowboard.board.dto.response.BoardAuditEventResponse>> getAuditEvents(
            @RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        return ResponseEntity.ok(boardService.getAuditEvents());
    }

    private void assertPlatformAdmin(String requesterRole) {
        if (!"PLATFORM_ADMIN".equalsIgnoreCase(requesterRole)) {
            throw new com.flowboard.board.exception.AccessDeniedException("Platform admin access required");
        }
    }
}
