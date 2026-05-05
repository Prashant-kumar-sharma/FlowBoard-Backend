package com.flowboard.workspace.service;
import com.flowboard.workspace.dto.request.*;
import com.flowboard.workspace.dto.response.*;
import java.util.List;
public interface WorkspaceService {
    WorkspaceResponse create(Long ownerId, CreateWorkspaceRequest req);
    WorkspaceResponse getById(Long id, Long userId);
    List<WorkspaceResponse> getByOwner(Long ownerId);
    List<WorkspaceResponse> getByMember(Long userId);
    List<WorkspaceResponse> getPublic();
    List<WorkspaceResponse> getAll();
    WorkspaceResponse update(Long id, Long userId, CreateWorkspaceRequest req);
    void delete(Long id, Long userId);
    void adminDelete(Long id, Long requesterId);
    MemberResponse addMember(Long workspaceId, Long requesterId, AddMemberRequest req);
    void removeMember(Long workspaceId, Long requesterId, Long targetUserId);
    void updateMemberRole(Long workspaceId, Long requesterId, Long targetUserId, String role);
    List<MemberResponse> getMembers(Long workspaceId);
    List<WorkspaceAuditEventResponse> getAuditEvents();
}
