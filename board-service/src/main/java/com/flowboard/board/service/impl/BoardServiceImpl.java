package com.flowboard.board.service.impl;

import com.flowboard.board.dto.request.CreateBoardRequest;
import com.flowboard.board.dto.response.BoardAuditEventResponse;
import com.flowboard.board.dto.response.BoardMemberResponse;
import com.flowboard.board.dto.response.BoardResponse;
import com.flowboard.board.entity.BoardAuditEvent;
import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.exception.AccessDeniedException;
import com.flowboard.board.exception.ResourceNotFoundException;
import com.flowboard.board.repository.BoardAuditEventRepository;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;
import com.flowboard.board.service.BoardService;
import com.flowboard.board.service.CardCleanupClient;
import com.flowboard.board.service.ListCleanupClient;
import com.flowboard.board.kafka.BoardEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor @Transactional
public class BoardServiceImpl implements BoardService {
    private static final String BOARD_TARGET_TYPE = "BOARD";
    private static final String BOARD_BY_ID_CACHE = "board:byId";
    private static final String BOARD_BY_WORKSPACE_CACHE = "board:byWorkspace";
    private static final String BOARD_BY_MEMBER_CACHE = "board:byMember";
    private static final String BOARD_ALL_CACHE = "board:all";
    private static final String BOARD_MEMBERS_CACHE = "board:members";

    private final BoardRepository boardRepository;
    private final BoardMemberRepository memberRepository;
    private final BoardAuditEventRepository auditEventRepository;
    private final BoardEventProducer eventProducer;
    private final ListCleanupClient listCleanupClient;
    private final CardCleanupClient cardCleanupClient;

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public BoardResponse create(CreateBoardRequest req, Long userId) {
        Board board = Board.builder()
                .name(req.getName()).description(req.getDescription())
                .background(req.getBackground()).visibility(req.getVisibility())
                .workspaceId(req.getWorkspaceId()).createdById(userId).build();
        Board saved = boardRepository.save(board);
        // Auto-add creator as ADMIN
        memberRepository.save(BoardMember.builder()
                .board(saved).userId(userId).role(BoardMember.Role.ADMIN).build());
        logAudit(saved.getId(), userId, "BOARD_CREATED", BOARD_TARGET_TYPE, String.valueOf(saved.getId()), saved.getName());
        return toResponse(saved);
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = BOARD_BY_ID_CACHE, key = "#id + ':' + (#userId == null ? 'guest' : #userId)")
    public BoardResponse getById(Long id, Long userId, String requesterRole) {
        Board b = find(id);
        
        // Security check: If private, must be a member
        if (b.getVisibility() == Board.Visibility.PRIVATE
                && !isPlatformAdmin(requesterRole)
                && (userId == null || !memberRepository.existsByBoardIdAndUserId(id, userId))) {
            throw new AccessDeniedException("Access denied to private board");
        }

        BoardResponse resp = BoardResponse.from(b);
        // Privacy check: Only show members to logged-in users
        if (userId != null) {
            resp.setMembers(memberRepository.findByBoardId(id).stream()
                    .map(BoardMemberResponse::from)
                    .collect(Collectors.toList()));
        } else {
            resp.setMembers(Collections.emptyList());
        }
        return resp;
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = BOARD_BY_WORKSPACE_CACHE, key = "#workspaceId + ':' + (#userId == null ? 'guest' : #userId)")
    public List<BoardResponse> getByWorkspace(Long workspaceId, Long userId) {
        List<Board> boards = boardRepository.findByWorkspaceId(workspaceId);
        if (userId == null) {
            // Guest: only see public boards
            return boards.stream()
                    .filter(b -> b.getVisibility() == Board.Visibility.PUBLIC)
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return boards.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = BOARD_BY_MEMBER_CACHE, key = "#userId")
    public List<BoardResponse> getByMember(Long userId) {
        return boardRepository.findByMemberUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = BOARD_ALL_CACHE)
    public List<BoardResponse> getAll() {
        return boardRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public BoardResponse update(Long id, CreateBoardRequest req, Long userId, String requesterRole) {
        Board board = find(id);
        assertAdmin(id, userId, requesterRole);
        if (req.getName() != null) board.setName(req.getName());
        if (req.getDescription() != null) board.setDescription(req.getDescription());
        if (req.getBackground() != null) board.setBackground(req.getBackground());
        if (req.getVisibility() != null) board.setVisibility(req.getVisibility());
        Board saved = boardRepository.save(board);
        logAudit(id, userId, "BOARD_UPDATED", BOARD_TARGET_TYPE, String.valueOf(id), saved.getName());
        return toResponse(saved);
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public void closeBoard(Long id, Long userId, String requesterRole) {
        Board board = find(id);
        assertAdmin(id, userId, requesterRole);
        board.setIsClosed(true);
        boardRepository.save(board);
        logAudit(id, userId, "BOARD_CLOSED", BOARD_TARGET_TYPE, String.valueOf(id), board.getName());
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public void deleteBoard(Long id, Long userId, String requesterRole) {
        Board board = find(id);
        if (!isPlatformAdmin(requesterRole) && !board.getCreatedById().equals(userId)) throw new AccessDeniedException("Only the creator can delete");
        deleteBoardWithDependents(board, userId, "BOARD_DELETED");
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public void adminCloseBoard(Long id, Long userId) {
        Board board = find(id);
        board.setIsClosed(true);
        boardRepository.save(board);
        logAudit(id, userId, "BOARD_CLOSED_BY_ADMIN", BOARD_TARGET_TYPE, String.valueOf(id), board.getName());
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public void adminDeleteBoard(Long id, Long userId) {
        Board board = find(id);
        deleteBoardWithDependents(board, userId, "BOARD_DELETED_BY_ADMIN");
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public void deleteByWorkspaceId(Long workspaceId, Long actorId) {
        boardRepository.findByWorkspaceId(workspaceId)
                .forEach(board -> deleteBoardWithDependents(board, actorId, "BOARD_DELETED_WITH_WORKSPACE"));
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public BoardMemberResponse addMember(Long boardId, Long userId, BoardMember.Role role, Long requesterId, String requesterRole) {
        Board board = find(boardId);
        assertAdmin(boardId, requesterId, requesterRole);
        BoardMember m = BoardMember.builder().board(board).userId(userId).role(role != null ? role : BoardMember.Role.MEMBER).build();
        BoardMember saved = memberRepository.save(m);
        logAudit(boardId, requesterId, "BOARD_MEMBER_ADDED", "USER", String.valueOf(userId), saved.getRole().name());

        // Fire invitation event for email notification
        try {
            eventProducer.sendMemberInvited(boardId, board.getName(), userId, requesterId, saved.getRole().name());
        } catch (Exception e) {
            log.warn("Failed to send board invitation event: {}", e.getMessage());
        }

        return BoardMemberResponse.from(saved);
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public BoardMemberResponse assignBoardAdmin(Long boardId, Long userId, Long requesterId) {
        Board board = find(boardId);
        BoardMember member = memberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseGet(() -> BoardMember.builder().board(board).userId(userId).build());

        member.setRole(BoardMember.Role.ADMIN);
        BoardMember saved = memberRepository.save(member);
        logAudit(boardId, requesterId, "BOARD_ADMIN_ASSIGNED_BY_PLATFORM_ADMIN", "USER", String.valueOf(userId), BoardMember.Role.ADMIN.name());

        return BoardMemberResponse.from(saved);
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public void removeMember(Long boardId, Long userId, Long requesterId, String requesterRole) {
        assertAdmin(boardId, requesterId, requesterRole);
        logAudit(boardId, requesterId, "BOARD_MEMBER_REMOVED", "USER", String.valueOf(userId), null);
        memberRepository.deleteByBoardIdAndUserId(boardId, userId);
    }

    @Override
    @CacheEvict(cacheNames = {
            BOARD_BY_ID_CACHE,
            BOARD_BY_WORKSPACE_CACHE,
            BOARD_BY_MEMBER_CACHE,
            BOARD_ALL_CACHE,
            BOARD_MEMBERS_CACHE
    }, allEntries = true)
    public BoardMemberResponse updateMemberRole(Long boardId, Long userId, BoardMember.Role role, Long requesterId, String requesterRole) {
        assertAdmin(boardId, requesterId, requesterRole);
        BoardMember m = memberRepository.findByBoardIdAndUserId(boardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Board member not found"));
        m.setRole(role);
        BoardMember saved = memberRepository.save(m);
        logAudit(boardId, requesterId, "BOARD_MEMBER_ROLE_UPDATED", "USER", String.valueOf(userId), role.name());
        return BoardMemberResponse.from(saved);
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = BOARD_MEMBERS_CACHE, key = "#boardId")
    public List<BoardMemberResponse> getMembers(Long boardId) {
        return memberRepository.findByBoardId(boardId).stream()
                .map(BoardMemberResponse::from)
                .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public List<BoardAuditEventResponse> getAuditEvents() {
        return auditEventRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BoardAuditEventResponse::from)
                .toList();
    }

    private Board find(Long id) {
        return boardRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Board not found: " + id));
    }

    private void assertAdmin(Long boardId, Long userId, String requesterRole) {
        if (isPlatformAdmin(requesterRole)) {
            return;
        }

        memberRepository.findByBoardIdAndUserId(boardId, userId)
                .filter(m -> m.getRole() == BoardMember.Role.ADMIN)
                .orElseThrow(() -> new AccessDeniedException("Board admin access required"));
    }

    private boolean isPlatformAdmin(String requesterRole) {
        return "PLATFORM_ADMIN".equalsIgnoreCase(requesterRole);
    }

    private BoardResponse toResponse(Board b) {
        BoardResponse r = BoardResponse.from(b);
        r.setMembers(memberRepository.findByBoardId(b.getId()).stream()
                .map(BoardMemberResponse::from)
                .collect(Collectors.toList()));
        return r;
    }

    private void logAudit(Long boardId, Long actorId, String action, String targetType, String targetId, String details) {
        auditEventRepository.save(BoardAuditEvent.builder()
                .boardId(boardId)
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build());
    }

    private void deleteBoardWithDependents(Board board, Long actorId, String auditAction) {
        cardCleanupClient.deleteByBoardId(board.getId());
        listCleanupClient.deleteByBoardId(board.getId());
        logAudit(board.getId(), actorId, auditAction, BOARD_TARGET_TYPE, String.valueOf(board.getId()), board.getName());
        boardRepository.delete(board);
    }
}
