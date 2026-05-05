package com.flowboard.label.repository;
import com.flowboard.label.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByChecklistIdOrderByPosition(Long checklistId);
    long countByChecklistId(Long checklistId);
    long countByChecklistIdAndIsCompleted(Long checklistId, Boolean completed);
}