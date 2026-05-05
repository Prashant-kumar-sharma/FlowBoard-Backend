package com.flowboard.card.service;
import com.flowboard.card.dto.request.CreateCardRequest;
import com.flowboard.card.dto.response.CardResponse;
import com.flowboard.card.entity.Card;
import com.flowboard.card.entity.CardActivity;
import java.util.List;

public interface CardService {
    CardResponse create(CreateCardRequest req, Long userId);
    CardResponse getById(Long id, Long userId);
    List<CardResponse> getByList(Long listId, Long userId);
    List<CardResponse> getByBoard(Long boardId, Long userId);
    List<CardResponse> getByAssignee(Long userId);
    List<CardResponse> getOverdue();
    List<CardResponse> getAll();
    CardResponse update(Long id, CreateCardRequest req, Long userId);
    CardResponse move(Long id, Long targetListId, Integer targetPosition, Long userId);
    void reorder(Long listId, List<Long> orderedIds, Long userId);
    void archive(Long id, Long userId);
    void unarchive(Long id, Long userId);
    void delete(Long id, Long userId);
    CardResponse setAssignee(Long id, Long assigneeId, Long userId);
    CardResponse setPriority(Long id, Card.Priority priority, Long userId);
    CardResponse setStatus(Long id, Card.Status status, Long userId);
    List<CardActivity> getActivity(Long id, Long userId);
    List<CardActivity> getAllActivity();
    void deleteByBoardId(Long boardId);
    void deleteByListId(Long listId);
}
