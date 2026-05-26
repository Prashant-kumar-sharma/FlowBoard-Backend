package com.flowboard.board.service;
import com.flowboard.board.dto.request.CreateBoardRequest;
import com.flowboard.board.dto.response.BoardAuditEventResponse;
import com.flowboard.board.dto.response.BoardMemberResponse;
import com.flowboard.board.dto.response.BoardResponse;
import com.flowboard.board.entity.BoardMember;
import java.util.List;

public interface BoardService {
    BoardResponse create(CreateBoardRequest req, Long userId);
    BoardResponse getById(Long id, Long userId, String requesterRole);
    List<BoardResponse> getByWorkspace(Long workspaceId, Long userId);
    List<BoardResponse> getByMember(Long userId);
    List<BoardResponse> getAll();
    BoardResponse update(Long id, CreateBoardRequest req, Long userId, String requesterRole);
    void closeBoard(Long id, Long userId, String requesterRole);
    void deleteBoard(Long id, Long userId, String requesterRole);
    void adminCloseBoard(Long id, Long userId);
    void adminDeleteBoard(Long id, Long userId);
    void deleteByWorkspaceId(Long workspaceId, Long actorId);
    BoardMemberResponse addMember(Long boardId, Long userId, BoardMember.Role role, Long requesterId, String requesterRole);
    BoardMemberResponse assignBoardAdmin(Long boardId, Long userId, Long requesterId);
    void removeMember(Long boardId, Long userId, Long requesterId, String requesterRole);
    BoardMemberResponse updateMemberRole(Long boardId, Long userId, BoardMember.Role role, Long requesterId, String requesterRole);
    List<BoardMemberResponse> getMembers(Long boardId);
    List<BoardAuditEventResponse> getAuditEvents();
}
