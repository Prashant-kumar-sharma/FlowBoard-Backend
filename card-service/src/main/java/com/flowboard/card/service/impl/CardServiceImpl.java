package com.flowboard.card.service.impl;

import com.flowboard.card.dto.request.CreateCardRequest;
import com.flowboard.card.dto.response.CardResponse;
import com.flowboard.card.entity.Card;
import com.flowboard.card.entity.CardActivity;
import com.flowboard.card.exception.ResourceNotFoundException;
import com.flowboard.card.repository.CardActivityRepository;
import com.flowboard.card.repository.CardRepository;
import com.flowboard.card.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor @Transactional
public class CardServiceImpl implements CardService {
    private static final String ACTION_ASSIGNED = "ASSIGNED";
    private static final String ACTION_CREATED = "CREATED";
    private static final String ACTION_UPDATED = "UPDATED";
    private static final String EVENT_CARD_ASSIGNED = "flowboard.card.assigned";
    private static final String FIELD_ASSIGNEE_ID = "assigneeId";
    private static final String JSON_KEY_ACTOR_ID = "\"actorId\":";
    private static final String JSON_KEY_ASSIGNEE_ID = "\"assigneeId\":";
    private static final String JSON_KEY_CARD_ID = "\"cardId\":";

    private final CardRepository cardRepository;
    private final CardActivityRepository activityRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public CardResponse create(CreateCardRequest req, Long userId) {
        int pos = (int)(cardRepository.countByListId(req.getListId()) + 1);
        Card card = Card.builder()
                .title(req.getTitle()).description(req.getDescription())
                .listId(req.getListId()).boardId(req.getBoardId())
                .position(pos).priority(req.getPriority())
                .dueDate(req.getDueDate()).startDate(req.getStartDate())
                .assigneeId(req.getAssigneeId()).createdById(userId)
                .coverColor(req.getCoverColor()).build();
        Card saved = cardRepository.save(card);
        logActivity(saved.getId(), userId, ACTION_CREATED, null, null, null);
        if (req.getAssigneeId() != null) {
            kafkaTemplate.send(EVENT_CARD_ASSIGNED,
                "{" + JSON_KEY_CARD_ID + saved.getId() + "," + JSON_KEY_ASSIGNEE_ID + req.getAssigneeId() + "," + JSON_KEY_ACTOR_ID + userId + "}");
        }
        return CardResponse.from(saved);
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "card:byId", key = "#id")
    public CardResponse getById(Long id, Long userId) { 
        log.info(">>> GET CARD: id={}, userId={}", id, userId);
        return CardResponse.from(find(id)); 
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "card:byList", key = "#listId")
    public List<CardResponse> getByList(Long listId, Long userId) {
        log.info(">>> GET CARDS BY LIST: listId={}, userId={}", listId, userId);
        return cardRepository.findByListIdOrderByPosition(listId).stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "card:byBoard", key = "#boardId")
    public List<CardResponse> getByBoard(Long boardId, Long userId) {
        log.info(">>> GET CARDS BY BOARD: boardId={}, userId={}", boardId, userId);
        return cardRepository.findByBoardId(boardId).stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "card:byAssignee", key = "#userId")
    public List<CardResponse> getByAssignee(Long userId) {
        return cardRepository.findByAssigneeId(userId).stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "card:overdue")
    public List<CardResponse> getOverdue() {
        return cardRepository.findByDueDateBeforeAndStatusNot(LocalDate.now(), Card.Status.DONE)
                .stream().map(CardResponse::from).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "card:all")
    public List<CardResponse> getAll() {
        return cardRepository.findAll().stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public CardResponse update(Long id, CreateCardRequest req, Long userId) {
        Card card = find(id);
        if (req.getTitle() != null) { logActivity(id, userId, ACTION_UPDATED, "title", card.getTitle(), req.getTitle()); card.setTitle(req.getTitle()); }
        if (req.getDescription() != null) card.setDescription(req.getDescription());
        if (req.getPriority() != null) { logActivity(id, userId, ACTION_UPDATED, "priority", card.getPriority().name(), req.getPriority().name()); card.setPriority(req.getPriority()); }
        if (req.getDueDate() != null) card.setDueDate(req.getDueDate());
        if (req.getAssigneeId() != null && !req.getAssigneeId().equals(card.getAssigneeId())) {
            String old = card.getAssigneeId() != null ? card.getAssigneeId().toString() : "none";
            card.setAssigneeId(req.getAssigneeId());
            logActivity(id, userId, ACTION_ASSIGNED, FIELD_ASSIGNEE_ID, old, req.getAssigneeId().toString());
            kafkaTemplate.send(EVENT_CARD_ASSIGNED,
                "{" + JSON_KEY_CARD_ID + id + "," + JSON_KEY_ASSIGNEE_ID + req.getAssigneeId() + "," + JSON_KEY_ACTOR_ID + userId + "}");
        }
        if (req.getCoverColor() != null) card.setCoverColor(req.getCoverColor());
        return CardResponse.from(cardRepository.save(card));
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public CardResponse move(Long id, Long targetListId, Integer targetPosition, Long userId) {
        Card card = find(id);
        Long oldListId = card.getListId();
        card.setListId(targetListId);
        card.setPosition(targetPosition);
        Card saved = cardRepository.save(card);
        logActivity(id, userId, "MOVED", "listId", oldListId.toString(), targetListId.toString());
        kafkaTemplate.send("flowboard.card.moved",
            "{\"cardId\":" + id + ",\"fromList\":" + oldListId + ",\"toList\":" + targetListId + ",\"actorId\":" + userId + "}");
        messagingTemplate.convertAndSend("/topic/board/" + card.getBoardId() + "/card-moved",
            "{\"cardId\":" + id + ",\"toList\":" + targetListId + "}");
        return CardResponse.from(saved);
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public void reorder(Long listId, List<Long> orderedIds, Long userId) {
        AtomicInteger pos = new AtomicInteger(1);
        orderedIds.forEach(cid -> { Card c = find(cid); c.setPosition(pos.getAndIncrement()); cardRepository.save(c); });
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public void archive(Long id, Long userId) {
        Card card = find(id); card.setIsArchived(true); cardRepository.save(card);
        logActivity(id, userId, "ARCHIVED", null, null, null);
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public void unarchive(Long id, Long userId) {
        Card card = find(id); card.setIsArchived(false); cardRepository.save(card);
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public void delete(Long id, Long userId) { cardRepository.delete(find(id)); }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public void deleteByBoardId(Long boardId) {
        List<Long> cardIds = cardRepository.findByBoardId(boardId).stream()
                .map(Card::getId)
                .collect(Collectors.toList());
        if (!cardIds.isEmpty()) {
            activityRepository.deleteByCardIdIn(cardIds);
        }
        cardRepository.deleteByBoardId(boardId);
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public void deleteByListId(Long listId) {
        List<Long> cardIds = cardRepository.findByListIdOrderByPosition(listId).stream()
                .map(Card::getId)
                .collect(Collectors.toList());
        if (!cardIds.isEmpty()) {
            activityRepository.deleteByCardIdIn(cardIds);
        }
        cardRepository.deleteByListId(listId);
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public CardResponse setAssignee(Long id, Long assigneeId, Long userId) {
        Card card = find(id);
        String old = card.getAssigneeId() != null ? card.getAssigneeId().toString() : "none";
        card.setAssigneeId(assigneeId);
        logActivity(id, userId, ACTION_ASSIGNED, FIELD_ASSIGNEE_ID, old, assigneeId != null ? assigneeId.toString() : "none");
        if (assigneeId != null) kafkaTemplate.send(EVENT_CARD_ASSIGNED,
            "{" + JSON_KEY_CARD_ID + id + "," + JSON_KEY_ASSIGNEE_ID + assigneeId + "," + JSON_KEY_ACTOR_ID + userId + "}");
        return CardResponse.from(cardRepository.save(card));
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public CardResponse setPriority(Long id, Card.Priority priority, Long userId) {
        Card card = find(id);
        logActivity(id, userId, ACTION_UPDATED, "priority", card.getPriority().name(), priority.name());
        card.setPriority(priority);
        return CardResponse.from(cardRepository.save(card));
    }

    @Override
    @CacheEvict(cacheNames = {"card:byId", "card:byList", "card:byBoard", "card:byAssignee", "card:overdue", "card:all", "card:activity"}, allEntries = true)
    public CardResponse setStatus(Long id, Card.Status status, Long userId) {
        Card card = find(id);
        logActivity(id, userId, ACTION_UPDATED, "status", card.getStatus().name(), status.name());
        card.setStatus(status);
        return CardResponse.from(cardRepository.save(card));
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "card:activity", key = "#id")
    public List<CardActivity> getActivity(Long id, Long userId) {
        log.info(">>> GET CARD ACTIVITY: id={}, userId={}", id, userId);
        return activityRepository.findByCardIdOrderByCreatedAtDesc(id);
    }

    @Override @Transactional(readOnly = true)
    public List<CardActivity> getAllActivity() {
        return activityRepository.findAllByOrderByCreatedAtDesc();
    }

    private Card find(Long id) {
        return cardRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Card not found: " + id));
    }

    private void logActivity(Long cardId, Long actorId, String action, String field, String oldVal, String newVal) {
        activityRepository.save(CardActivity.builder()
                .cardId(cardId).actorId(actorId).action(action)
                .fieldName(field).oldValue(oldVal).newValue(newVal).build());
    }
}
