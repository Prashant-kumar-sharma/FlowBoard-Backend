package com.flowboard.label.repository;
import com.flowboard.label.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
    List<Checklist> findByCardIdOrderByPosition(Long cardId);
}