package com.flowboard.label.service.impl;

import com.flowboard.label.entity.*;
import com.flowboard.label.exception.ResourceNotFoundException;
import com.flowboard.label.repository.*;
import com.flowboard.label.service.LabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor @Transactional
public class LabelServiceImpl implements LabelService {
    private final LabelRepository labelRepository;
    private final CardLabelRepository cardLabelRepository;
    private final ChecklistRepository checklistRepository;
    private final ChecklistItemRepository itemRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Label createLabel(Long boardId, String name, String color) {
        return labelRepository.save(Label.builder().boardId(boardId).name(name).color(color).build());
    }

    @Override @Transactional(readOnly = true)
    public List<Label> getLabelsByBoard(Long boardId) { return labelRepository.findByBoardId(boardId); }

    @Override
    public Label updateLabel(Long id, String name, String color) {
        Label label = labelRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Label not found"));
        if (name != null) label.setName(name);
        if (color != null) label.setColor(color);
        return labelRepository.save(label);
    }

    @Override
    public void deleteLabel(Long id) { labelRepository.deleteById(id); }

    @Override
    public CardLabel addLabelToCard(Long cardId, Long labelId) {
        if (cardLabelRepository.existsByCardIdAndLabelId(cardId, labelId))
            return cardLabelRepository.findByCardId(cardId).stream()
                    .filter(cl -> cl.getLabelId().equals(labelId)).findFirst().orElseThrow();
        return cardLabelRepository.save(CardLabel.builder().cardId(cardId).labelId(labelId).build());
    }

    @Override
    public void removeLabelFromCard(Long cardId, Long labelId) {
        cardLabelRepository.deleteByCardIdAndLabelId(cardId, labelId);
    }

    @Override @Transactional(readOnly = true)
    public List<Label> getLabelsForCard(Long cardId) {
        return cardLabelRepository.findByCardId(cardId).stream()
                .map(cl -> labelRepository.findById(cl.getLabelId()).orElse(null))
                .filter(l -> l != null).toList();
    }

    @Override
    public Checklist createChecklist(Long cardId, String title) {
        int pos = checklistRepository.findByCardIdOrderByPosition(cardId).size() + 1;
        return checklistRepository.save(Checklist.builder().cardId(cardId).title(title).position(pos).build());
    }

    @Override
    public ChecklistItem addItem(Long checklistId, String text, Long assigneeId) {
        Checklist checklist = checklistRepository.findById(checklistId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist not found"));
        int pos = (int)(itemRepository.countByChecklistId(checklistId) + 1);
        return itemRepository.save(ChecklistItem.builder()
                .checklist(checklist).text(text).assigneeId(assigneeId).position(pos).build());
    }

    @Override
    public ChecklistItem toggleItem(Long itemId) {
        ChecklistItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        item.setIsCompleted(!item.getIsCompleted());
        ChecklistItem saved = itemRepository.save(item);
        long checklistId = item.getChecklist().getId();
        int progress = calculateChecklistProgress(checklistId);
        messagingTemplate.convertAndSend("/topic/checklist/" + checklistId + "/updated",
            "{\"progress\":" + progress + "}");
        return saved;
    }

    @Override
    public void deleteChecklist(Long id) { checklistRepository.deleteById(id); }

    @Override @Transactional(readOnly = true)
    public List<Checklist> getChecklistsByCard(Long cardId) {
        return checklistRepository.findByCardIdOrderByPosition(cardId);
    }

    @Override @Transactional(readOnly = true)
    public int getChecklistProgress(Long checklistId) {
        return calculateChecklistProgress(checklistId);
    }

    private int calculateChecklistProgress(Long checklistId) {
        long total = itemRepository.countByChecklistId(checklistId);
        if (total == 0) return 0;
        long done = itemRepository.countByChecklistIdAndIsCompleted(checklistId, true);
        return (int)((done * 100) / total);
    }
}
