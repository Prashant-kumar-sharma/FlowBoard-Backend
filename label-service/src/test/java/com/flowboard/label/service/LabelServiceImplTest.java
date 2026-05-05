package com.flowboard.label.service;

import com.flowboard.label.entity.CardLabel;
import com.flowboard.label.entity.Checklist;
import com.flowboard.label.entity.ChecklistItem;
import com.flowboard.label.entity.Label;
import com.flowboard.label.exception.ResourceNotFoundException;
import com.flowboard.label.repository.CardLabelRepository;
import com.flowboard.label.repository.ChecklistItemRepository;
import com.flowboard.label.repository.ChecklistRepository;
import com.flowboard.label.repository.LabelRepository;
import com.flowboard.label.service.impl.LabelServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelServiceImplTest {

    @Mock
    private LabelRepository labelRepository;
    @Mock
    private CardLabelRepository cardLabelRepository;
    @Mock
    private ChecklistRepository checklistRepository;
    @Mock
    private ChecklistItemRepository itemRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private LabelServiceImpl labelService;

    private Label label;
    private Checklist checklist;
    private ChecklistItem item;

    @BeforeEach
    void setUp() {
        label = Label.builder().id(1L).boardId(10L).name("Bug").color("#f00").build();
        checklist = Checklist.builder().id(5L).cardId(100L).title("Done").position(1).build();
        item = ChecklistItem.builder().id(9L).checklist(checklist).text("Ship it").isCompleted(false).position(1).build();
    }

    @Test
    void createLabelPersistsNewLabel() {
        when(labelRepository.save(any(Label.class))).thenReturn(label);

        Label saved = labelService.createLabel(10L, "Bug", "#f00");

        assertThat(saved.getBoardId()).isEqualTo(10L);
        assertThat(saved.getName()).isEqualTo("Bug");
    }

    @Test
    void updateLabelAppliesProvidedFields() {
        when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
        when(labelRepository.save(label)).thenReturn(label);

        Label updated = labelService.updateLabel(1L, "Feature", null);

        assertThat(updated.getName()).isEqualTo("Feature");
        assertThat(updated.getColor()).isEqualTo("#f00");
    }

    @Test
    void updateLabelChangesColorOnly() {
        when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
        when(labelRepository.save(label)).thenReturn(label);

        Label updated = labelService.updateLabel(1L, null, "#0f0");

        assertThat(updated.getName()).isEqualTo("Bug");
        assertThat(updated.getColor()).isEqualTo("#0f0");
    }

    @Test
    void getLabelsByBoardReturnsRepositoryResults() {
        when(labelRepository.findByBoardId(10L)).thenReturn(List.of(label));

        assertThat(labelService.getLabelsByBoard(10L)).containsExactly(label);
    }

    @Test
    void deleteLabelAndRemoveLabelFromCardDelegateToRepositories() {
        labelService.deleteLabel(1L);
        labelService.removeLabelFromCard(100L, 1L);

        verify(labelRepository).deleteById(1L);
        verify(cardLabelRepository).deleteByCardIdAndLabelId(100L, 1L);
    }

    @Test
    void addLabelToCardReturnsExistingRelationWhenAlreadyPresent() {
        CardLabel existing = CardLabel.builder().cardId(100L).labelId(1L).build();
        when(cardLabelRepository.existsByCardIdAndLabelId(100L, 1L)).thenReturn(true);
        when(cardLabelRepository.findByCardId(100L)).thenReturn(List.of(existing));

        CardLabel result = labelService.addLabelToCard(100L, 1L);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void addLabelToCardCreatesNewRelationWhenMissing() {
        CardLabel saved = CardLabel.builder().cardId(100L).labelId(1L).build();
        when(cardLabelRepository.existsByCardIdAndLabelId(100L, 1L)).thenReturn(false);
        when(cardLabelRepository.save(any(CardLabel.class))).thenReturn(saved);

        CardLabel result = labelService.addLabelToCard(100L, 1L);

        assertThat(result).isSameAs(saved);
    }

    @Test
    void getLabelsForCardFiltersMissingLabels() {
        when(cardLabelRepository.findByCardId(100L)).thenReturn(List.of(
                CardLabel.builder().cardId(100L).labelId(1L).build(),
                CardLabel.builder().cardId(100L).labelId(2L).build()));
        when(labelRepository.findById(1L)).thenReturn(Optional.of(label));
        when(labelRepository.findById(2L)).thenReturn(Optional.empty());

        List<Label> result = labelService.getLabelsForCard(100L);

        assertThat(result).containsExactly(label);
    }

    @Test
    void createChecklistAppendsToEnd() {
        when(checklistRepository.findByCardIdOrderByPosition(100L)).thenReturn(List.of(checklist));
        when(checklistRepository.save(any(Checklist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Checklist created = labelService.createChecklist(100L, "Follow up");

        assertThat(created.getPosition()).isEqualTo(2);
    }

    @Test
    void getChecklistsByCardReturnsRepositoryResults() {
        when(checklistRepository.findByCardIdOrderByPosition(100L)).thenReturn(List.of(checklist));

        assertThat(labelService.getChecklistsByCard(100L)).containsExactly(checklist);
    }

    @Test
    void addItemThrowsWhenChecklistMissing() {
        when(checklistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labelService.addItem(99L, "Missing", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addItemAssignsNextPosition() {
        when(checklistRepository.findById(5L)).thenReturn(Optional.of(checklist));
        when(itemRepository.countByChecklistId(5L)).thenReturn(2L);
        when(itemRepository.save(any(ChecklistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChecklistItem saved = labelService.addItem(5L, "Review", 42L);

        assertThat(saved.getPosition()).isEqualTo(3);
        assertThat(saved.getAssigneeId()).isEqualTo(42L);
    }

    @Test
    void toggleItemUpdatesProgressAndBroadcasts() {
        when(itemRepository.findById(9L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(itemRepository.countByChecklistId(5L)).thenReturn(4L);
        when(itemRepository.countByChecklistIdAndIsCompleted(5L, true)).thenReturn(3L);

        ChecklistItem result = labelService.toggleItem(9L);

        assertThat(result.getIsCompleted()).isTrue();
        verify(messagingTemplate).convertAndSend("/topic/checklist/5/updated", "{\"progress\":75}");
    }

    @Test
    void getChecklistProgressReturnsZeroWhenNoItems() {
        when(itemRepository.countByChecklistId(5L)).thenReturn(0L);

        assertThat(labelService.getChecklistProgress(5L)).isZero();
    }

    @Test
    void deleteChecklistDelegatesToRepository() {
        labelService.deleteChecklist(5L);

        verify(checklistRepository).deleteById(5L);
    }
}
