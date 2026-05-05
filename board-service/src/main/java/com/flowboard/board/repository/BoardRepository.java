package com.flowboard.board.repository;
import com.flowboard.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByWorkspaceId(Long workspaceId);
    List<Board> findByCreatedById(Long userId);
    List<Board> findByVisibility(Board.Visibility visibility);
    @Query("SELECT b FROM Board b JOIN b.members m WHERE m.userId = :userId")
    List<Board> findByMemberUserId(Long userId);
    List<Board> findByWorkspaceIdAndIsClosed(Long workspaceId, Boolean isClosed);
}