package com.flowboard.list.repository;
import com.flowboard.list.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TaskListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findByBoardIdOrderByPosition(Long boardId);
    List<TaskList> findByBoardIdAndIsArchivedOrderByPosition(Long boardId, Boolean isArchived);
    long countByBoardId(Long boardId);
    java.util.Optional<Integer> findMaxPositionByBoardId(Long boardId);
    void deleteByBoardId(Long boardId);
}
