package com.flowboard.workspace.service.impl;
import com.flowboard.workspace.dto.request.*;
import com.flowboard.workspace.dto.response.*;
import com.flowboard.workspace.entity.*;
import com.flowboard.workspace.exception.*;
import com.flowboard.workspace.repository.*;
import com.flowboard.workspace.service.BoardCleanupClient;
import com.flowboard.workspace.service.PaymentEntitlementClient;
import com.flowboard.workspace.service.WorkspaceService;
import com.flowboard.workspace.kafka.WorkspaceEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor @Transactional
public class WorkspaceServiceImpl implements WorkspaceService {
    private static final String WORKSPACE_TARGET_TYPE = "WORKSPACE";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceAuditEventRepository auditEventRepository;
    private final WorkspaceEventProducer eventProducer;
    private final PaymentEntitlementClient paymentEntitlementClient;
    private final BoardCleanupClient boardCleanupClient;

    @Value("${app.plan.free-workspace-limit:5}")
    private int freeWorkspaceLimit;

    @Value("${app.plan.free-member-limit:5}")
    private int freeMemberLimit;

    @Override
    @CacheEvict(cacheNames = {"workspace:byId", "workspace:byOwner", "workspace:byMember", "workspace:public", "workspace:all", "workspace:members"}, allEntries = true)
    public WorkspaceResponse create(Long ownerId, CreateWorkspaceRequest req) {
        enforceWorkspaceLimit(ownerId);
        Workspace workspace = Workspace.builder()
            .name(req.getName()).description(req.getDescription())
            .ownerId(ownerId).logoUrl(req.getLogoUrl())
            .visibility(Workspace.Visibility.valueOf(req.getVisibility()))
            .build();
        Workspace saved = workspaceRepository.save(workspace);
        WorkspaceMember owner = WorkspaceMember.builder()
            .workspace(saved).userId(ownerId).role(WorkspaceMember.Role.ADMIN).build();
        memberRepository.save(owner);
        logAudit(saved.getId(), ownerId, "WORKSPACE_CREATED", WORKSPACE_TARGET_TYPE, String.valueOf(saved.getId()), saved.getName());
        log.info("Workspace created: {} by user {}", saved.getId(), ownerId);
        return WorkspaceResponse.from(saved);
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "workspace:byId", key = "#id + ':' + (#userId == null ? 'guest' : #userId)")
    public WorkspaceResponse getById(Long id, Long userId, String requesterRole) {
        Workspace w = findById(id);
        
        // Security check: If private, must be a member
        if (w.getVisibility() == Workspace.Visibility.PRIVATE
                && !isPlatformAdmin(requesterRole)
                && (userId == null || !memberRepository.existsByWorkspaceIdAndUserId(id, userId))) {
            throw new UnauthorizedException("Access denied to private workspace");
        }

        WorkspaceResponse resp = WorkspaceResponse.from(w);
        // Privacy check: Only show members to logged-in users
        if (userId != null) {
            resp.setMembers(memberRepository.findByWorkspaceId(id).stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList()));
        } else {
            resp.setMembers(Collections.emptyList());
        }
        return resp;
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "workspace:byOwner", key = "#ownerId")
    public List<WorkspaceResponse> getByOwner(Long ownerId) {
        return workspaceRepository.findByOwnerId(ownerId).stream()
            .map(WorkspaceResponse::from)
            .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "workspace:byMember", key = "#userId")
    public List<WorkspaceResponse> getByMember(Long userId) {
        return workspaceRepository.findByMemberUserId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "workspace:public")
    public List<WorkspaceResponse> getPublic() {
        return workspaceRepository.findByVisibility(Workspace.Visibility.PUBLIC).stream()
            .map(w -> {
                WorkspaceResponse resp = WorkspaceResponse.from(w);
                resp.setMembers(Collections.emptyList());
                return resp;
            })
            .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "workspace:all")
    public List<WorkspaceResponse> getAll() {
        return workspaceRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {"workspace:byId", "workspace:byOwner", "workspace:byMember", "workspace:public", "workspace:all", "workspace:members"}, allEntries = true)
    public WorkspaceResponse update(Long id, Long userId, String requesterRole, CreateWorkspaceRequest req) {
        Workspace w = findById(id);
        assertAdmin(id, userId, requesterRole);
        w.setName(req.getName());
        if (req.getDescription() != null) w.setDescription(req.getDescription());
        if (req.getLogoUrl() != null) w.setLogoUrl(req.getLogoUrl());
        if (req.getVisibility() != null) w.setVisibility(Workspace.Visibility.valueOf(req.getVisibility()));
        Workspace saved = workspaceRepository.save(w);
        logAudit(id, userId, "WORKSPACE_UPDATED", WORKSPACE_TARGET_TYPE, String.valueOf(id), saved.getName());
        return WorkspaceResponse.from(saved);
    }

    @Override
    @CacheEvict(cacheNames = {"workspace:byId", "workspace:byOwner", "workspace:byMember", "workspace:public", "workspace:all", "workspace:members"}, allEntries = true)
    public void delete(Long id, Long userId, String requesterRole) {
        Workspace w = findById(id);
        if (!isPlatformAdmin(requesterRole) && !w.getOwnerId().equals(userId)) throw new UnauthorizedException("Only owner can delete workspace");
        boardCleanupClient.deleteByWorkspaceId(id, userId);
        logAudit(id, userId, "WORKSPACE_DELETED", WORKSPACE_TARGET_TYPE, String.valueOf(id), w.getName());
        workspaceRepository.delete(w);
    }

    @Override
    @CacheEvict(cacheNames = {"workspace:byId", "workspace:byOwner", "workspace:byMember", "workspace:public", "workspace:all", "workspace:members"}, allEntries = true)
    public void adminDelete(Long id, Long requesterId) {
        Workspace workspace = findById(id);
        boardCleanupClient.deleteByWorkspaceId(id, requesterId);
        logAudit(id, requesterId, "WORKSPACE_DELETED_BY_ADMIN", WORKSPACE_TARGET_TYPE, String.valueOf(id), workspace.getName());
        workspaceRepository.delete(workspace);
    }

    @Override
    @CacheEvict(cacheNames = {"workspace:byId", "workspace:byOwner", "workspace:byMember", "workspace:public", "workspace:all", "workspace:members"}, allEntries = true)
    public MemberResponse addMember(Long workspaceId, Long requesterId, String requesterRole, AddMemberRequest req) {
        assertAdmin(workspaceId, requesterId, requesterRole);
        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, req.getUserId()))
            throw new DuplicateResourceException("User is already a member");
        Workspace ws = findById(workspaceId);
        enforceMemberLimit(ws);
        WorkspaceMember member = WorkspaceMember.builder()
            .workspace(ws).userId(req.getUserId())
            .role(WorkspaceMember.Role.valueOf(req.getRole())).build();
        WorkspaceMember saved = memberRepository.save(member);
        logAudit(workspaceId, requesterId, "MEMBER_ADDED", "USER", String.valueOf(req.getUserId()), req.getRole());

        // Fire invitation event for email notification
        try {
            eventProducer.sendMemberInvited(workspaceId, ws.getName(), req.getUserId(), requesterId, req.getRole());
        } catch (Exception e) {
            log.warn("Failed to send workspace invitation event: {}", e.getMessage());
        }

        return MemberResponse.from(saved);
    }

    @Override
    @CacheEvict(cacheNames = {"workspace:byId", "workspace:byOwner", "workspace:byMember", "workspace:public", "workspace:all", "workspace:members"}, allEntries = true)
    public void removeMember(Long workspaceId, Long requesterId, String requesterRole, Long targetUserId) {
        assertAdmin(workspaceId, requesterId, requesterRole);
        logAudit(workspaceId, requesterId, "MEMBER_REMOVED", "USER", String.valueOf(targetUserId), null);
        memberRepository.deleteByWorkspaceIdAndUserId(workspaceId, targetUserId);
    }

    @Override
    @CacheEvict(cacheNames = {"workspace:byId", "workspace:byOwner", "workspace:byMember", "workspace:public", "workspace:all", "workspace:members"}, allEntries = true)
    public void updateMemberRole(Long workspaceId, Long requesterId, String requesterRole, Long targetUserId, String role) {
        assertAdmin(workspaceId, requesterId, requesterRole);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        member.setRole(WorkspaceMember.Role.valueOf(role));
        memberRepository.save(member);
        logAudit(workspaceId, requesterId, "MEMBER_ROLE_UPDATED", "USER", String.valueOf(targetUserId), role);
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "workspace:members", key = "#workspaceId")
    public List<MemberResponse> getMembers(Long workspaceId) {
        return memberRepository.findByWorkspaceId(workspaceId).stream()
            .map(MemberResponse::from)
            .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public List<WorkspaceAuditEventResponse> getAuditEvents() {
        return auditEventRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(WorkspaceAuditEventResponse::from)
            .toList();
    }

    private Workspace findById(Long id) {
        return workspaceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + id));
    }

    private void assertAdmin(Long workspaceId, Long userId, String requesterRole) {
        if (isPlatformAdmin(requesterRole)) {
            return;
        }

        memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .filter(m -> m.getRole() == WorkspaceMember.Role.ADMIN)
            .orElseThrow(() -> new UnauthorizedException("Admin access required"));
    }

    private boolean isPlatformAdmin(String requesterRole) {
        return "PLATFORM_ADMIN".equalsIgnoreCase(requesterRole);
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        WorkspaceResponse response = WorkspaceResponse.from(workspace);
        response.setMembers(memberRepository.findByWorkspaceId(workspace.getId()).stream()
            .map(MemberResponse::from)
            .collect(Collectors.toList()));
        return response;
    }

    private void enforceWorkspaceLimit(Long ownerId) {
        long ownedWorkspaces = workspaceRepository.countByOwnerId(ownerId);
        if (ownedWorkspaces >= freeWorkspaceLimit && !paymentEntitlementClient.isPremium(ownerId)) {
            throw new PaymentRequiredException(
                "Free plan includes up to 5 workspaces. Upgrade to premium to create more.",
                "WORKSPACE_LIMIT",
                freeWorkspaceLimit,
                ownedWorkspaces,
                "/billing/premium?reason=workspace-limit",
                ownerId,
                null
            );
        }
    }

    private void enforceMemberLimit(Workspace workspace) {
        long memberCount = memberRepository.countByWorkspaceId(workspace.getId());
        if (memberCount >= freeMemberLimit && !paymentEntitlementClient.isPremium(workspace.getOwnerId())) {
            throw new PaymentRequiredException(
                "Free plan includes up to 5 members per workspace. Upgrade to premium to invite more teammates.",
                "MEMBER_LIMIT",
                freeMemberLimit,
                memberCount,
                "/billing/premium?reason=member-limit&workspaceId=" + workspace.getId(),
                workspace.getOwnerId(),
                workspace.getId()
            );
        }
    }

    private void logAudit(Long workspaceId, Long actorId, String action, String targetType, String targetId, String details) {
        auditEventRepository.save(WorkspaceAuditEvent.builder()
            .workspaceId(workspaceId)
            .actorId(actorId)
            .action(action)
            .targetType(targetType)
            .targetId(targetId)
            .details(details)
            .build());
    }
}
