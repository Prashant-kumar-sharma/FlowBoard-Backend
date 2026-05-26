package com.flowboard.workspace.controller;

import com.flowboard.workspace.dto.request.CreateWorkspaceRequest;
import com.flowboard.workspace.dto.response.MemberResponse;
import com.flowboard.workspace.dto.response.WorkspaceResponse;
import com.flowboard.workspace.service.WorkspaceService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspaces", description = "Workspace management")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @Operation(summary = "Create a workspace")
    public ResponseEntity<WorkspaceResponse> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workspaceService.create(userId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workspace by ID")
    public ResponseEntity<WorkspaceResponse> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole) {
        return ResponseEntity.ok(workspaceService.getById(id, userId, requesterRole));
    }

    @GetMapping("/my")
    @Operation(summary = "Get workspaces where user is a member")
    public ResponseEntity<List<WorkspaceResponse>> getMy(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(workspaceService.getByMember(userId));
    }

    @GetMapping("/public")
    @Operation(summary = "Get all public workspaces")
    public ResponseEntity<List<WorkspaceResponse>> getPublic() {
        return ResponseEntity.ok(workspaceService.getPublic());
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Platform admin - list all workspaces")
    public ResponseEntity<List<WorkspaceResponse>> getAllForAdmin(
            @RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        return ResponseEntity.ok(workspaceService.getAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update workspace")
    public ResponseEntity<WorkspaceResponse> update(
            @PathVariable Long id,
            @RequestBody CreateWorkspaceRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole) {
        return ResponseEntity.ok(workspaceService.update(id, userId, requesterRole, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete workspace")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole) {
        workspaceService.delete(id, userId, requesterRole);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/{id}")
    @Operation(summary = "Platform admin - delete workspace")
    public ResponseEntity<Void> adminDelete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        workspaceService.adminDelete(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add member to workspace")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable Long id,
            @RequestBody com.flowboard.workspace.dto.request.AddMemberRequest request,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workspaceService.addMember(id, requesterId, requesterRole, request));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove member from workspace")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole) {
        workspaceService.removeMember(id, requesterId, requesterRole, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/members/{userId}/role")
    @Operation(summary = "Update member role")
    public ResponseEntity<Void> updateRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @RequestBody com.flowboard.workspace.dto.request.UpdateRoleRequest request,
            @RequestHeader("X-User-Id") Long requesterId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole) {
        workspaceService.updateMemberRole(id, requesterId, requesterRole, userId, request.getRole());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List workspace members")
    public ResponseEntity<List<MemberResponse>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.getMembers(id));
    }

    @GetMapping("/admin/audit")
    @Operation(summary = "Platform admin - list workspace audit events")
    public ResponseEntity<List<com.flowboard.workspace.dto.response.WorkspaceAuditEventResponse>> getAuditEvents(
            @RequestHeader("X-User-Role") String requesterRole) {
        assertPlatformAdmin(requesterRole);
        return ResponseEntity.ok(workspaceService.getAuditEvents());
    }

    private void assertPlatformAdmin(String requesterRole) {
        if (!"PLATFORM_ADMIN".equalsIgnoreCase(requesterRole)) {
            throw new com.flowboard.workspace.exception.UnauthorizedException("Platform admin access required");
        }
    }
}
