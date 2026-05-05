package com.flowboard.label.service;
import com.flowboard.label.entity.*;
import java.util.List;
public interface LabelService {
    Label createLabel(Long boardId, String name, String color);
    List<Label> getLabelsByBoard(Long boardId);
    Label updateLabel(Long id, String name, String color);
    void deleteLabel(Long id);
    CardLabel addLabelToCard(Long cardId, Long labelId);
    void removeLabelFromCard(Long cardId, Long labelId);
    List<Label> getLabelsForCard(Long cardId);
    Checklist createChecklist(Long cardId, String title);
    ChecklistItem addItem(Long checklistId, String text, Long assigneeId);
    ChecklistItem toggleItem(Long itemId);
    void deleteChecklist(Long id);
    List<Checklist> getChecklistsByCard(Long cardId);
    int getChecklistProgress(Long checklistId);
}