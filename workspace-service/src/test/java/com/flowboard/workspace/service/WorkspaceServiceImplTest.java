package com.flowboard.workspace.service;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.flowboard.workspace.dto.request.AddMemberRequest;
import com.flowboard.workspace.dto.request.CreateWorkspaceRequest;
import com.flowboard.workspace.dto.response.MemberResponse;
import com.flowboard.workspace.dto.response.WorkspaceResponse;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.exception.ResourceNotFoundException;
import com.flowboard.workspace.exception.DuplicateResourceException;
import com.flowboard.workspace.exception.PaymentRequiredException;
import com.flowboard.workspace.exception.UnauthorizedException;
import com.flowboard.workspace.repository.WorkspaceAuditEventRepository;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import com.flowboard.workspace.kafka.WorkspaceEventProducer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;
import com.flowboard.workspace.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceImplTest {

    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock WorkspaceAuditEventRepository auditEventRepository;
    @Mock WorkspaceEventProducer eventProducer;
    @Mock PaymentEntitlementClient paymentEntitlementClient;
    @Mock BoardCleanupClient boardCleanupClient;
    @InjectMocks WorkspaceServiceImpl workspaceService;

    private Workspace sampleWorkspace;
    private WorkspaceMember adminMember;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workspaceService, "freeWorkspaceLimit", 5);
        ReflectionTestUtils.setField(workspaceService, "freeMemberLimit", 5);
        sampleWorkspace = Workspace.builder()
                .id(1L).name("Test WS").ownerId(10L)
                .visibility(Workspace.Visibility.PRIVATE).build();
        adminMember = WorkspaceMember.builder()
                .id(1L).workspace(sampleWorkspace)
                .userId(10L).role(WorkspaceMember.Role.ADMIN).build();
    }

    @Test
    void should_createWorkspace_and_addOwnerAsAdmin() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("My Workspace");
        req.setVisibility("PRIVATE");

        when(workspaceRepository.countByOwnerId(10L)).thenReturn(0L);
        when(workspaceRepository.save(any())).thenReturn(sampleWorkspace);
        when(memberRepository.save(any())).thenReturn(adminMember);

        WorkspaceResponse response = workspaceService.create(10L, req);

        assertThat(response.getName()).isEqualTo("Test WS");

        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(WorkspaceMember.Role.ADMIN);
    }

    @Test
    void should_throwResourceNotFoundException_when_workspaceNotFound() {
        when(workspaceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> workspaceService.getById(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_returnWorkspaceWithMembers_forLoggedInMember() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(sampleWorkspace));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 10L)).thenReturn(true);
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember));

        WorkspaceResponse response = workspaceService.getById(1L, 10L);

        assertThat(response.getMembers()).hasSize(1);
    }

    @Test
    void should_returnWorkspaceWithoutMembers_forGuestOnPublicWorkspace() {
        sampleWorkspace.setVisibility(Workspace.Visibility.PUBLIC);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(sampleWorkspace));

        WorkspaceResponse response = workspaceService.getById(1L, null);

        assertThat(response.getMembers()).isEmpty();
    }

    @Test
    void should_returnWorkspacesByOwner() {
        when(workspaceRepository.findByOwnerId(10L)).thenReturn(List.of(sampleWorkspace));

        assertThat(workspaceService.getByOwner(10L)).hasSize(1);
    }

    @Test
    void should_returnWorkspacesByMember() {
        when(workspaceRepository.findByMemberUserId(10L)).thenReturn(List.of(sampleWorkspace));
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember));

        assertThat(workspaceService.getByMember(10L)).hasSize(1);
    }

    @Test
    void should_roundTripCachedWorkspaceLists_throughRedisSerializer() {
        when(workspaceRepository.findByMemberUserId(10L)).thenReturn(List.of(sampleWorkspace));
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember));

        List<WorkspaceResponse> workspaces = workspaceService.getByMember(10L);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer()
                .configure(objectMapper -> {
                    objectMapper.findAndRegisterModules();
                    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                });

        Object roundTrip = serializer.deserialize(serializer.serialize(workspaces));

        assertThat(roundTrip).isInstanceOf(List.class);
    }

    @Test
    void should_returnAllWorkspaces() {
        when(workspaceRepository.findAll()).thenReturn(List.of(sampleWorkspace));
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember));

        assertThat(workspaceService.getAll()).hasSize(1);
    }

    @Test
    void should_deleteWorkspace_when_requesterIsOwner() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(sampleWorkspace));
        workspaceService.delete(1L, 10L);
        verify(boardCleanupClient).deleteByWorkspaceId(1L, 10L);
        verify(workspaceRepository).delete(sampleWorkspace);
    }

    @Test
    void should_throwAccessDenied_when_nonOwnerTriesToDelete() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(sampleWorkspace));
        assertThatThrownBy(() -> workspaceService.delete(1L, 99L))
                .isInstanceOf(UnauthorizedException.class);
        verify(workspaceRepository, never()).delete(any());
    }

    @Test
    void should_addMember_when_requesterIsAdmin() {
        Workspace ws = sampleWorkspace;
        WorkspaceMember newMember = WorkspaceMember.builder()
                .workspace(ws).userId(20L).role(WorkspaceMember.Role.MEMBER).build();

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(ws));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 20L)).thenReturn(false);
        when(memberRepository.countByWorkspaceId(1L)).thenReturn(4L);
        when(memberRepository.save(any())).thenReturn(newMember);

        AddMemberRequest addReq = new AddMemberRequest();
        addReq.setUserId(20L);
        addReq.setRole("MEMBER");

        MemberResponse response = workspaceService.addMember(1L, 10L, addReq);
        assertThat(response).isNotNull();
        verify(memberRepository).save(any());
    }

    @Test
    void should_throwAccessDenied_when_nonAdminAddsMembers() {
        WorkspaceMember regularMember = WorkspaceMember.builder()
                .workspace(sampleWorkspace).userId(99L).role(WorkspaceMember.Role.MEMBER).build();
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 99L)).thenReturn(Optional.of(regularMember));

        AddMemberRequest addReq = new AddMemberRequest();
        addReq.setUserId(20L);

        assertThatThrownBy(() -> workspaceService.addMember(1L, 99L, addReq))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void should_throwDuplicateResourceException_when_userAlreadyMember() {
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 20L)).thenReturn(true);

        AddMemberRequest addReq = new AddMemberRequest();
        addReq.setUserId(20L);
        addReq.setRole("MEMBER");

        assertThatThrownBy(() -> workspaceService.addMember(1L, 10L, addReq))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("User is already a member");
        verify(memberRepository, never()).save(any());
    }

    @Test
    void should_returnPublicWorkspaces() {
        when(workspaceRepository.findByVisibility(Workspace.Visibility.PUBLIC))
                .thenReturn(List.of(sampleWorkspace));

        List<WorkspaceResponse> result = workspaceService.getPublic();
        assertThat(result).hasSize(1);
    }

    @Test
    void should_updateWorkspace_when_requesterIsAdmin() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Renamed");
        req.setDescription("Updated desc");
        req.setLogoUrl("/logo.png");
        req.setVisibility("PUBLIC");
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(sampleWorkspace));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(workspaceRepository.save(sampleWorkspace)).thenReturn(sampleWorkspace);

        WorkspaceResponse response = workspaceService.update(1L, 10L, req);

        assertThat(response.getName()).isEqualTo("Renamed");
        assertThat(response.getDescription()).isEqualTo("Updated desc");
        assertThat(response.getLogoUrl()).isEqualTo("/logo.png");
        assertThat(response.getVisibility()).isEqualTo("PUBLIC");
    }

    @Test
    void should_adminDeleteWorkspace() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(sampleWorkspace));

        workspaceService.adminDelete(1L, 99L);

        verify(boardCleanupClient).deleteByWorkspaceId(1L, 99L);
        verify(workspaceRepository).delete(sampleWorkspace);
    }

    @Test
    void should_requirePayment_when_creatingSixthWorkspace_withoutPremium() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setName("Overflow");
        req.setVisibility("PRIVATE");

        when(workspaceRepository.countByOwnerId(10L)).thenReturn(5L);
        when(paymentEntitlementClient.isPremium(10L)).thenReturn(false);

        assertThatThrownBy(() -> workspaceService.create(10L, req))
                .isInstanceOf(PaymentRequiredException.class)
                .hasMessageContaining("up to 5 workspaces");
    }

    @Test
    void should_requirePayment_when_addingSixthMember_withoutPremium() {
        AddMemberRequest addReq = new AddMemberRequest();
        addReq.setUserId(20L);
        addReq.setRole("MEMBER");

        when(memberRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.existsByWorkspaceIdAndUserId(1L, 20L)).thenReturn(false);
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(sampleWorkspace));
        when(memberRepository.countByWorkspaceId(1L)).thenReturn(5L);
        when(paymentEntitlementClient.isPremium(10L)).thenReturn(false);

        assertThatThrownBy(() -> workspaceService.addMember(1L, 10L, addReq))
                .isInstanceOf(PaymentRequiredException.class)
                .hasMessageContaining("up to 5 members");
    }

    @Test
    void should_removeMember_when_requesterIsAdmin() {
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));

        workspaceService.removeMember(1L, 10L, 20L);

        verify(memberRepository).deleteByWorkspaceIdAndUserId(1L, 20L);
    }

    @Test
    void should_updateMemberRole_when_memberExists() {
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(sampleWorkspace).userId(20L).role(WorkspaceMember.Role.MEMBER).build();
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);

        workspaceService.updateMemberRole(1L, 10L, 20L, "ADMIN");

        assertThat(member.getRole()).isEqualTo(WorkspaceMember.Role.ADMIN);
    }

    @Test
    void should_throwWhenUpdatingMissingMemberRole() {
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByWorkspaceIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.updateMemberRole(1L, 10L, 20L, "ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_returnMembersForWorkspace() {
        when(memberRepository.findByWorkspaceId(1L)).thenReturn(List.of(adminMember));

        assertThat(workspaceService.getMembers(1L)).hasSize(1);
    }

    @Test
    void should_returnAuditEvents() {
        when(auditEventRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(workspaceService.getAuditEvents()).isEmpty();
    }
}
