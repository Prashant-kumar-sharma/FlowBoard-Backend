package com.flowboard.card.service;

import com.flowboard.card.dto.request.CreateCardRequest;
import com.flowboard.card.dto.response.CardResponse;
import com.flowboard.card.entity.Card;
import com.flowboard.card.entity.CardActivity;
import com.flowboard.card.exception.ResourceNotFoundException;
import com.flowboard.card.repository.CardActivityRepository;
import com.flowboard.card.repository.CardRepository;
import com.flowboard.card.service.impl.CardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock CardRepository cardRepository;
    @Mock CardActivityRepository activityRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks CardServiceImpl cardService;

    private Card sampleCard;

    @BeforeEach
    void setUp() {
        sampleCard = Card.builder()
                .id(1L).title("Test Card").listId(10L).boardId(5L)
                .position(1).priority(Card.Priority.MEDIUM)
                .status(Card.Status.TO_DO).isArchived(false)
                .coverColor("#FFFFFF").createdById(1L).build();
    }

    @Test
    void should_createCard_with_correctPosition() {
        CreateCardRequest req = new CreateCardRequest();
        req.setTitle("New Card"); req.setListId(10L); req.setBoardId(5L);
        when(cardRepository.countByListId(10L)).thenReturn(2L);
        when(cardRepository.save(any())).thenReturn(sampleCard);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        CardResponse result = cardService.create(req, 1L);
        assertThat(result.getTitle()).isEqualTo("Test Card");

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        assertThat(captor.getValue().getPosition()).isEqualTo(3);
    }

    @Test
    void should_publishKafkaEvent_when_assigneeSetOnCreate() {
        CreateCardRequest req = new CreateCardRequest();
        req.setTitle("Card"); req.setListId(10L); req.setBoardId(5L); req.setAssigneeId(42L);
        when(cardRepository.countByListId(any())).thenReturn(0L);
        when(cardRepository.save(any())).thenReturn(sampleCard);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.create(req, 1L);
        verify(kafkaTemplate).send(eq("flowboard.card.assigned"), anyString());
    }

    @Test
    void should_updateAssignee_when_presentInUpdateRequest() {
        CreateCardRequest req = new CreateCardRequest();
        req.setAssigneeId(42L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));
        when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        CardResponse result = cardService.update(1L, req, 7L);

        assertThat(result.getAssigneeId()).isEqualTo(42L);
        verify(cardRepository).save(argThat(card -> Long.valueOf(42L).equals(card.getAssigneeId())));
        verify(activityRepository).save(argThat(activity ->
                "ASSIGNED".equals(activity.getAction()) && "assigneeId".equals(activity.getFieldName())));
        verify(kafkaTemplate).send(eq("flowboard.card.assigned"), anyString());
    }

    @Test
    void should_throwResourceNotFoundException_when_cardNotFound() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cardService.getById(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_moveCard_and_broadcastViaWebSocket() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));
        when(cardRepository.save(any())).thenReturn(sampleCard);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.move(1L, 20L, 1, 1L);
        verify(messagingTemplate).convertAndSend(contains("/card-moved"), anyString());
        verify(kafkaTemplate).send(eq("flowboard.card.moved"), anyString());
    }

    @Test
    void should_flagAsOverdue_when_dueDatePastAndNotDone() {
        sampleCard.setDueDate(LocalDate.now().minusDays(1));
        sampleCard.setStatus(Card.Status.IN_PROGRESS);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));

        CardResponse result = cardService.getById(1L, 1L);
        assertThat(result.getIsOverdue()).isTrue();
    }

    @Test
    void should_notFlagAsOverdue_when_statusIsDone() {
        sampleCard.setDueDate(LocalDate.now().minusDays(1));
        sampleCard.setStatus(Card.Status.DONE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));

        CardResponse result = cardService.getById(1L, 1L);
        assertThat(result.getIsOverdue()).isFalse();
    }

    @Test
    void should_returnCardsByListBoardAndAssignee() {
        when(cardRepository.findByListIdOrderByPosition(10L)).thenReturn(List.of(sampleCard));
        when(cardRepository.findByBoardId(5L)).thenReturn(List.of(sampleCard));
        when(cardRepository.findByAssigneeId(42L)).thenReturn(List.of(sampleCard));

        assertThat(cardService.getByList(10L, 1L)).hasSize(1);
        assertThat(cardService.getByBoard(5L, 1L)).hasSize(1);
        assertThat(cardService.getByAssignee(42L)).hasSize(1);
    }

    @Test
    void should_returnOverdueAndAllCards() {
        when(cardRepository.findByDueDateBeforeAndStatusNot(any(), eq(Card.Status.DONE))).thenReturn(List.of(sampleCard));
        when(cardRepository.findAll()).thenReturn(List.of(sampleCard));

        assertThat(cardService.getOverdue()).hasSize(1);
        assertThat(cardService.getAll()).hasSize(1);
    }

    @Test
    void should_archiveCard_and_logActivity() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));
        when(cardRepository.save(any())).thenReturn(sampleCard);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.archive(1L, 1L);
        verify(cardRepository).save(argThat(c -> c.getIsArchived()));
    }

    @Test
    void should_setPriority_and_logActivity() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));
        when(cardRepository.save(any())).thenReturn(sampleCard);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        cardService.setPriority(1L, Card.Priority.CRITICAL, 1L);
        verify(cardRepository).save(argThat(c -> c.getPriority() == Card.Priority.CRITICAL));
        verify(activityRepository).save(argThat(a ->
                "UPDATED".equals(a.getAction()) && "priority".equals(a.getFieldName())));
    }

    @Test
    void should_setStatus_and_logActivity() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));
        when(cardRepository.save(any())).thenReturn(sampleCard);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        CardResponse result = cardService.setStatus(1L, Card.Status.DONE, 1L);

        assertThat(result.getStatus()).isEqualTo(Card.Status.DONE);
        verify(activityRepository).save(argThat(a ->
                "UPDATED".equals(a.getAction()) && "status".equals(a.getFieldName())));
    }

    @Test
    void should_setAssigneeToNullWithoutKafkaPublish() {
        sampleCard.setAssigneeId(42L);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));
        when(cardRepository.save(any())).thenReturn(sampleCard);
        when(activityRepository.save(any())).thenReturn(new CardActivity());

        CardResponse result = cardService.setAssignee(1L, null, 1L);

        assertThat(result.getAssigneeId()).isNull();
        verify(cardRepository).save(argThat(card -> card.getAssigneeId() == null));
    }

    @Test
    void should_unarchiveAndDeleteCard() {
        sampleCard.setIsArchived(true);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(sampleCard));
        when(cardRepository.save(any())).thenReturn(sampleCard);

        cardService.unarchive(1L, 1L);
        cardService.delete(1L, 1L);

        assertThat(sampleCard.getIsArchived()).isFalse();
        verify(cardRepository).delete(sampleCard);
    }

    @Test
    void should_deleteCardsAndActivityByBoardId() {
        Card secondCard = Card.builder().id(2L).boardId(5L).listId(10L).build();
        when(cardRepository.findByBoardId(5L)).thenReturn(List.of(sampleCard, secondCard));

        cardService.deleteByBoardId(5L);

        verify(activityRepository).deleteByCardIdIn(List.of(1L, 2L));
        verify(cardRepository).deleteByBoardId(5L);
    }

    @Test
    void should_deleteCardsAndActivityByListId() {
        Card secondCard = Card.builder().id(2L).boardId(5L).listId(10L).position(2).build();
        when(cardRepository.findByListIdOrderByPosition(10L)).thenReturn(List.of(sampleCard, secondCard));

        cardService.deleteByListId(10L);

        verify(activityRepository).deleteByCardIdIn(List.of(1L, 2L));
        verify(cardRepository).deleteByListId(10L);
    }

    @Test
    void should_reorderCards_in_correct_sequence() {
        Card c1 = Card.builder().id(1L).listId(10L).position(1).build();
        Card c2 = Card.builder().id(2L).listId(10L).position(2).build();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(c1));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(c2));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        cardService.reorder(10L, List.of(2L, 1L), 1L);

        verify(cardRepository).save(argThat(c -> c.getId().equals(2L) && c.getPosition() == 1));
        verify(cardRepository).save(argThat(c -> c.getId().equals(1L) && c.getPosition() == 2));
    }

    @Test
    void should_returnActivityFeeds() {
        CardActivity activity = CardActivity.builder().cardId(1L).action("CREATED").build();
        when(activityRepository.findByCardIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(activity));
        when(activityRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(activity));

        assertThat(cardService.getActivity(1L, 1L)).containsExactly(activity);
        assertThat(cardService.getAllActivity()).containsExactly(activity);
    }
}
