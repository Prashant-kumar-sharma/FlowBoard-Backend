package com.flowboard.list.service;

import com.flowboard.list.dto.request.CreateListRequest;
import com.flowboard.list.dto.response.ListResponse;
import com.flowboard.list.entity.TaskList;
import com.flowboard.list.exception.ResourceNotFoundException;
import com.flowboard.list.repository.TaskListRepository;
import com.flowboard.list.service.impl.ListServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListServiceImplTest {

    @Mock
    private TaskListRepository listRepository;
    @Mock
    private CardCleanupClient cardCleanupClient;

    @InjectMocks
    private ListServiceImpl listService;

    private TaskList taskList;

    @BeforeEach
    void setUp() {
        taskList = TaskList.builder()
                .id(1L)
                .boardId(10L)
                .name("Todo")
                .color("#fff")
                .position(1)
                .isArchived(false)
                .build();
    }

    @Test
    void createAssignsNextPosition() {
        CreateListRequest request = new CreateListRequest();
        request.setBoardId(10L);
        request.setName("Todo");
        request.setColor("#fff");

        when(listRepository.countByBoardId(10L)).thenReturn(2L);
        when(listRepository.save(any(TaskList.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ListResponse response = listService.create(request, 99L);

        assertThat(response.getPosition()).isEqualTo(3);
        assertThat(response.getBoardId()).isEqualTo(10L);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(listRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> listService.getById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void getByIdReturnsMappedList() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));

        ListResponse response = listService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Todo");
    }

    @Test
    void getByBoardMapsResults() {
        when(listRepository.findByBoardIdAndIsArchivedOrderByPosition(10L, false))
                .thenReturn(List.of(taskList));

        List<ListResponse> response = listService.getByBoard(10L, 1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getName()).isEqualTo("Todo");
    }

    @Test
    void getArchivedByBoardMapsArchivedLists() {
        taskList.setIsArchived(true);
        when(listRepository.findByBoardIdAndIsArchivedOrderByPosition(10L, true))
                .thenReturn(List.of(taskList));

        List<ListResponse> response = listService.getArchivedByBoard(10L, 1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getIsArchived()).isTrue();
    }

    @Test
    void updateChangesProvidedFieldsOnly() {
        CreateListRequest request = new CreateListRequest();
        request.setName("Doing");

        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));
        when(listRepository.save(taskList)).thenReturn(taskList);

        ListResponse response = listService.update(1L, request, 1L);

        assertThat(response.getName()).isEqualTo("Doing");
        assertThat(taskList.getColor()).isEqualTo("#fff");
    }

    @Test
    void reorderUpdatesPositionsInOrder() {
        TaskList second = TaskList.builder().id(2L).boardId(10L).name("Done").position(2).build();
        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));
        when(listRepository.findById(2L)).thenReturn(Optional.of(second));

        listService.reorder(10L, List.of(2L, 1L), 1L);

        assertThat(second.getPosition()).isEqualTo(1);
        assertThat(taskList.getPosition()).isEqualTo(2);
        verify(listRepository).save(second);
        verify(listRepository).save(taskList);
    }

    @Test
    void archiveMarksListArchived() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));

        listService.archive(1L, 1L);

        assertThat(taskList.getIsArchived()).isTrue();
        verify(listRepository).save(taskList);
    }

    @Test
    void unarchiveMarksListActive() {
        taskList.setIsArchived(true);
        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));

        listService.unarchive(1L, 1L);

        assertThat(taskList.getIsArchived()).isFalse();
        verify(listRepository).save(taskList);
    }

    @Test
    void moveChangesBoardAndAppendsPosition() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));
        when(listRepository.countByBoardId(20L)).thenReturn(4L);
        when(listRepository.save(taskList)).thenReturn(taskList);

        ListResponse response = listService.move(1L, 20L, 1L);

        assertThat(response.getBoardId()).isEqualTo(20L);
        assertThat(response.getPosition()).isEqualTo(5);
    }

    @Test
    void deleteRemovesExistingList() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(taskList));

        listService.delete(1L, 1L);

        verify(cardCleanupClient).deleteByListId(1L);
        verify(listRepository).delete(taskList);
    }

    @Test
    void deleteByBoardRemovesAllListsForBoard() {
        listService.deleteByBoardId(10L);

        verify(listRepository).deleteByBoardId(10L);
    }
}
