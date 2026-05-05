package com.flowboard.list.service;
import com.flowboard.list.dto.request.CreateListRequest;
import com.flowboard.list.dto.response.ListResponse;
import java.util.List;
public interface ListService {
    ListResponse create(CreateListRequest req, Long userId);
    ListResponse getById(Long id);
    List<ListResponse> getByBoard(Long boardId, Long userId);
    List<ListResponse> getArchivedByBoard(Long boardId, Long userId);
    ListResponse update(Long id, CreateListRequest req, Long userId);
    void reorder(Long boardId, List<Long> orderedIds, Long userId);
    void archive(Long id, Long userId);
    void unarchive(Long id, Long userId);
    void delete(Long id, Long userId);
    ListResponse move(Long id, Long targetBoardId, Long userId);
    void deleteByBoardId(Long boardId);
}
