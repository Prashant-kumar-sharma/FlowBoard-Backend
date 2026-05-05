package com.flowboard.label.repository;
import com.flowboard.label.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findByBoardId(Long boardId);
}