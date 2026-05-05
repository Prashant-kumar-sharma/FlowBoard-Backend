package com.flowboard.card.repository;
import com.flowboard.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByListIdOrderByPosition(Long listId);
    List<Card> findByBoardId(Long boardId);
    List<Card> findByAssigneeId(Long userId);
    List<Card> findByBoardIdAndIsArchived(Long boardId, Boolean archived);
    List<Card> findByDueDateBeforeAndStatusNot(LocalDate date, Card.Status status);
    List<Card> findByListIdAndIsArchived(Long listId, Boolean archived);
    long countByListId(Long listId);
    void deleteByBoardId(Long boardId);
    void deleteByListId(Long listId);
}
