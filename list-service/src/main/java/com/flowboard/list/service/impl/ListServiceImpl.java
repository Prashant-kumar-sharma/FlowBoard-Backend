package com.flowboard.list.service.impl;

import com.flowboard.list.dto.request.CreateListRequest;
import com.flowboard.list.dto.response.ListResponse;
import com.flowboard.list.entity.TaskList;
import com.flowboard.list.exception.ResourceNotFoundException;
import com.flowboard.list.repository.TaskListRepository;
import com.flowboard.list.service.CardCleanupClient;
import com.flowboard.list.service.ListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j @Service @RequiredArgsConstructor @Transactional
public class ListServiceImpl implements ListService {
    private static final String LIST_NOT_FOUND_PREFIX = "List not found: ";

    private final TaskListRepository listRepository;
    private final CardCleanupClient cardCleanupClient;

    @Override
    @CacheEvict(cacheNames = {"list:byId", "list:byBoard", "list:archivedByBoard"}, allEntries = true)
    public ListResponse create(CreateListRequest req, Long userId) {
        log.info(">>> CREATE LIST: name='{}', boardId={}, userId={}", req.getName(), req.getBoardId(), userId);
        int nextPos = (int) (listRepository.countByBoardId(req.getBoardId()) + 1);
        TaskList list = TaskList.builder()
                .boardId(req.getBoardId())
                .name(req.getName())
                .color(req.getColor())
                .position(nextPos)
                .isArchived(false)
                .build();
        TaskList saved = listRepository.save(list);
        log.info(">>> LIST SAVED: id={}, boardId={}, position={}", saved.getId(), saved.getBoardId(), saved.getPosition());
        return ListResponse.from(saved);
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "list:byId", key = "#id")
    public ListResponse getById(Long id) {
        TaskList list = listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LIST_NOT_FOUND_PREFIX + id));
        return ListResponse.from(list);
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "list:byBoard", key = "#boardId")
    public List<ListResponse> getByBoard(Long boardId, Long userId) {
        log.info(">>> GET LISTS BY BOARD: boardId={}, userId={}", boardId, userId);
        List<TaskList> lists = listRepository.findByBoardIdAndIsArchivedOrderByPosition(boardId, false);
        log.info(">>> FOUND {} lists for boardId={}: {}", lists.size(), boardId, 
            lists.stream().map(l -> "[id=" + l.getId() + ",name=" + l.getName() + ",boardId=" + l.getBoardId() + "]").collect(Collectors.joining(", ")));
        return lists.stream().map(ListResponse::from).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    @Cacheable(cacheNames = "list:archivedByBoard", key = "#boardId")
    public List<ListResponse> getArchivedByBoard(Long boardId, Long userId) {
        return listRepository.findByBoardIdAndIsArchivedOrderByPosition(boardId, true)
                .stream().map(ListResponse::from).collect(Collectors.toList());
    }

    @Override
    @CacheEvict(cacheNames = {"list:byId", "list:byBoard", "list:archivedByBoard"}, allEntries = true)
    public ListResponse update(Long id, CreateListRequest req, Long userId) {
        TaskList list = listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LIST_NOT_FOUND_PREFIX + id));
        if (req.getName() != null) list.setName(req.getName());
        if (req.getColor() != null) list.setColor(req.getColor());
        return ListResponse.from(listRepository.save(list));
    }

    @Override
    @CacheEvict(cacheNames = {"list:byId", "list:byBoard", "list:archivedByBoard"}, allEntries = true)
    public void reorder(Long boardId, List<Long> orderedIds, Long userId) {
        AtomicInteger pos = new AtomicInteger(1);
        orderedIds.forEach(lid -> {
            TaskList l = listRepository.findById(lid)
                    .orElseThrow(() -> new ResourceNotFoundException(LIST_NOT_FOUND_PREFIX + lid));
            l.setPosition(pos.getAndIncrement());
            listRepository.save(l);
        });
    }

    @Override
    @CacheEvict(cacheNames = {"list:byId", "list:byBoard", "list:archivedByBoard"}, allEntries = true)
    public void archive(Long id, Long userId) {
        TaskList list = listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LIST_NOT_FOUND_PREFIX + id));
        list.setIsArchived(true);
        listRepository.save(list);
    }

    @Override
    @CacheEvict(cacheNames = {"list:byId", "list:byBoard", "list:archivedByBoard"}, allEntries = true)
    public void unarchive(Long id, Long userId) {
        TaskList list = listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LIST_NOT_FOUND_PREFIX + id));
        list.setIsArchived(false);
        listRepository.save(list);
    }

    @Override
    @CacheEvict(cacheNames = {"list:byId", "list:byBoard", "list:archivedByBoard"}, allEntries = true)
    public void delete(Long id, Long userId) {
        TaskList list = listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LIST_NOT_FOUND_PREFIX + id));
        cardCleanupClient.deleteByListId(id);
        listRepository.delete(list);
    }

    @Override
    @CacheEvict(cacheNames = {"list:byId", "list:byBoard", "list:archivedByBoard"}, allEntries = true)
    public ListResponse move(Long id, Long targetBoardId, Long userId) {
        TaskList list = listRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LIST_NOT_FOUND_PREFIX + id));
        int nextPos = (int)(listRepository.countByBoardId(targetBoardId) + 1);
        list.setBoardId(targetBoardId);
        list.setPosition(nextPos);
        return ListResponse.from(listRepository.save(list));
    }

    @Override
    @CacheEvict(cacheNames = {"list:byId", "list:byBoard", "list:archivedByBoard"}, allEntries = true)
    public void deleteByBoardId(Long boardId) {
        listRepository.deleteByBoardId(boardId);
    }
}
