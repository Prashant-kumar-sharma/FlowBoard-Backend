package com.flowboard.workspace.service;
import com.flowboard.workspace.dto.request.*;
import com.flowboard.workspace.dto.response.*;
import java.util.List;
public interface WorkspaceService {
    WorkspaceResponse create(Long ownerId, CreateWorkspaceRequest req);
    WorkspaceResponse getById(Long id, Long userId, String requesterRole);
    List<WorkspaceResponse> getByOwner(Long ownerId);
    List<WorkspaceResponse> getByMember(Long userId);
    List<WorkspaceResponse> getPublic();
    List<WorkspaceResponse> getAll();
    WorkspaceResponse update(Long id, Long userId, String requesterRole, CreateWorkspaceRequest req);
    void delete(Long id, Long userId, String requesterRole);
    void adminDelete(Long id, Long requesterId);
    MemberResponse addMember(Long workspaceId, Long requesterId, String requesterRole, AddMemberRequest req);
    void removeMember(Long workspaceId, Long requesterId, String requesterRole, Long targetUserId);
    void updateMemberRole(Long workspaceId, Long requesterId, String requesterRole, Long targetUserId, String role);
    List<MemberResponse> getMembers(Long workspaceId);
    List<WorkspaceAuditEventResponse> getAuditEvents();
}
