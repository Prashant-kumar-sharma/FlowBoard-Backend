package com.flowboard.board.service;

import com.flowboard.board.dto.request.CreateBoardRequest;
import com.flowboard.board.dto.response.BoardMemberResponse;
import com.flowboard.board.dto.response.BoardResponse;
import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.exception.AccessDeniedException;
import com.flowboard.board.exception.ResourceNotFoundException;
import com.flowboard.board.kafka.BoardEventProducer;
import com.flowboard.board.repository.BoardAuditEventRepository;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;
import com.flowboard.board.service.impl.BoardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    @Mock
    private BoardRepository boardRepository;
    @Mock
    private BoardMemberRepository memberRepository;
    @Mock
    private BoardAuditEventRepository auditEventRepository;
    @Mock
    private BoardEventProducer eventProducer;
    @Mock
    private ListCleanupClient listCleanupClient;
    @Mock
    private CardCleanupClient cardCleanupClient;

    @InjectMocks
    private BoardServiceImpl boardService;

    private Board board;
    private BoardMember adminMember;

    @BeforeEach
    void setUp() {
        board = Board.builder()
                .id(1L)
                .name("Roadmap")
                .workspaceId(10L)
                .createdById(7L)
                .visibility(Board.Visibility.PRIVATE)
                .isClosed(false)
                .build();
        adminMember = BoardMember.builder()
                .id(11L)
                .board(board)
                .userId(7L)
                .role(BoardMember.Role.ADMIN)
                .build();
    }

    @Test
    void createAddsCreatorAsAdmin() {
        CreateBoardRequest request = new CreateBoardRequest();
        request.setName("Roadmap");
        request.setWorkspaceId(10L);
        request.setVisibility(Board.Visibility.PRIVATE);

        when(boardRepository.save(any(Board.class))).thenReturn(board);
        when(memberRepository.save(any(BoardMember.class))).thenReturn(adminMember);
        when(memberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember));

        BoardResponse response = boardService.create(request, 7L);

        assertThat(response.getName()).isEqualTo("Roadmap");
        verify(memberRepository).save(any(BoardMember.class));
    }

    @Test
    void getByIdRejectsGuestForPrivateBoard() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> boardService.getById(1L, null, "MEMBER"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByWorkspaceShowsOnlyPublicBoardsToGuests() {
        Board publicBoard = Board.builder()
                .id(2L)
                .name("Public")
                .workspaceId(10L)
                .visibility(Board.Visibility.PUBLIC)
                .build();
        when(boardRepository.findByWorkspaceId(10L)).thenReturn(List.of(board, publicBoard));
        when(memberRepository.findByBoardId(2L)).thenReturn(List.of());

        List<BoardResponse> response = boardService.getByWorkspace(10L, null);

        assertThat(response).extracting(BoardResponse::getName).containsExactly("Public");
    }

    @Test
    void getByIdReturnsMembersForLoggedInUser() {
        Board publicBoard = Board.builder()
                .id(2L)
                .name("Public")
                .visibility(Board.Visibility.PUBLIC)
                .build();
        BoardMember member = BoardMember.builder().board(publicBoard).userId(7L).role(BoardMember.Role.ADMIN).build();
        when(boardRepository.findById(2L)).thenReturn(Optional.of(publicBoard));
        when(memberRepository.findByBoardId(2L)).thenReturn(List.of(member));

        BoardResponse response = boardService.getById(2L, 7L, "MEMBER");

        assertThat(response.getMembers()).hasSize(1);
    }

    @Test
    void getByMemberMapsBoards() {
        when(boardRepository.findByMemberUserId(7L)).thenReturn(List.of(board));
        when(memberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember));

        assertThat(boardService.getByMember(7L)).hasSize(1);
    }

    @Test
    void getAllMapsBoards() {
        when(boardRepository.findAll()).thenReturn(List.of(board));
        when(memberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember));

        assertThat(boardService.getAll()).hasSize(1);
    }

    @Test
    void updateRequiresAdmin() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.update(1L, new CreateBoardRequest(), 9L, "MEMBER"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updatePersistsProvidedChanges() {
        CreateBoardRequest request = new CreateBoardRequest();
        request.setName("Updated");
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 7L)).thenReturn(Optional.of(adminMember));
        when(boardRepository.save(board)).thenReturn(board);
        when(memberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember));

        BoardResponse response = boardService.update(1L, request, 7L, "MEMBER");

        assertThat(response.getName()).isEqualTo("Updated");
    }

    @Test
    void closeBoardMarksBoardClosed() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 7L)).thenReturn(Optional.of(adminMember));

        boardService.closeBoard(1L, 7L, "MEMBER");

        assertThat(board.getIsClosed()).isTrue();
        verify(boardRepository).save(board);
    }

    @Test
    void deleteBoardRequiresCreator() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        assertThatThrownBy(() -> boardService.deleteBoard(1L, 99L, "MEMBER"))
                .isInstanceOf(AccessDeniedException.class);
        verify(boardRepository, never()).delete(any(Board.class));
    }

    @Test
    void deleteBoardDeletesWhenRequesterIsCreator() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        boardService.deleteBoard(1L, 7L, "MEMBER");

        verify(cardCleanupClient).deleteByBoardId(1L);
        verify(listCleanupClient).deleteByBoardId(1L);
        verify(boardRepository).delete(board);
    }

    @Test
    void adminCloseBoardClosesWithoutMembershipCheck() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        boardService.adminCloseBoard(1L, 99L);

        assertThat(board.getIsClosed()).isTrue();
        verify(boardRepository).save(board);
    }

    @Test
    void adminDeleteBoardDeletesWithoutMembershipCheck() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));

        boardService.adminDeleteBoard(1L, 99L);

        verify(cardCleanupClient).deleteByBoardId(1L);
        verify(listCleanupClient).deleteByBoardId(1L);
        verify(boardRepository).delete(board);
    }

    @Test
    void deleteByWorkspaceIdDeletesEveryBoardInWorkspace() {
        Board anotherBoard = Board.builder()
                .id(2L)
                .name("Backlog")
                .workspaceId(10L)
                .createdById(7L)
                .visibility(Board.Visibility.PRIVATE)
                .build();
        when(boardRepository.findByWorkspaceId(10L)).thenReturn(List.of(board, anotherBoard));

        boardService.deleteByWorkspaceId(10L, 77L);

        verify(cardCleanupClient).deleteByBoardId(1L);
        verify(cardCleanupClient).deleteByBoardId(2L);
        verify(listCleanupClient).deleteByBoardId(1L);
        verify(listCleanupClient).deleteByBoardId(2L);
        verify(boardRepository).delete(board);
        verify(boardRepository).delete(anotherBoard);
    }

    @Test
    void addMemberPublishesInvitationBestEffort() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 7L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.save(any(BoardMember.class)))
                .thenReturn(BoardMember.builder().board(board).userId(8L).role(BoardMember.Role.MEMBER).build());
        doThrow(new RuntimeException("kafka down"))
                .when(eventProducer).sendMemberInvited(1L, "Roadmap", 8L, 7L, "MEMBER");

        BoardMemberResponse response = boardService.addMember(1L, 8L, BoardMember.Role.MEMBER, 7L, "MEMBER");

        assertThat(response.getUserId()).isEqualTo(8L);
    }

    @Test
    void assignBoardAdminAddsMissingMemberAsAdmin() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 8L)).thenReturn(Optional.empty());
        when(memberRepository.save(any(BoardMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BoardMemberResponse response = boardService.assignBoardAdmin(1L, 8L, 99L);

        assertThat(response.getUserId()).isEqualTo(8L);
        assertThat(response.getRole()).isEqualTo(BoardMember.Role.ADMIN);
    }

    @Test
    void assignBoardAdminPromotesExistingMember() {
        BoardMember member = BoardMember.builder().board(board).userId(8L).role(BoardMember.Role.MEMBER).build();
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(memberRepository.findByBoardIdAndUserId(1L, 8L)).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);

        BoardMemberResponse response = boardService.assignBoardAdmin(1L, 8L, 99L);

        assertThat(response.getRole()).isEqualTo(BoardMember.Role.ADMIN);
    }

    @Test
    void removeMemberDeletesRelation() {
        when(memberRepository.findByBoardIdAndUserId(1L, 7L)).thenReturn(Optional.of(adminMember));

        boardService.removeMember(1L, 8L, 7L, "MEMBER");

        verify(memberRepository).deleteByBoardIdAndUserId(1L, 8L);
    }

    @Test
    void updateMemberRoleThrowsWhenMemberMissing() {
        when(memberRepository.findByBoardIdAndUserId(1L, 7L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByBoardIdAndUserId(1L, 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardService.updateMemberRole(1L, 8L, BoardMember.Role.ADMIN, 7L, "MEMBER"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMemberRoleSavesUpdatedRole() {
        BoardMember member = BoardMember.builder().board(board).userId(8L).role(BoardMember.Role.MEMBER).build();
        when(memberRepository.findByBoardIdAndUserId(1L, 7L)).thenReturn(Optional.of(adminMember));
        when(memberRepository.findByBoardIdAndUserId(1L, 8L)).thenReturn(Optional.of(member));
        when(memberRepository.save(member)).thenReturn(member);

        BoardMemberResponse response = boardService.updateMemberRole(1L, 8L, BoardMember.Role.ADMIN, 7L, "MEMBER");

        assertThat(response.getRole()).isEqualTo(BoardMember.Role.ADMIN);
    }

    @Test
    void getMembersAndAuditEventsReturnMappedResults() {
        when(memberRepository.findByBoardId(1L)).thenReturn(List.of(adminMember));
        when(auditEventRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(boardService.getMembers(1L)).hasSize(1);
        assertThat(boardService.getAuditEvents()).isEmpty();
    }
}

