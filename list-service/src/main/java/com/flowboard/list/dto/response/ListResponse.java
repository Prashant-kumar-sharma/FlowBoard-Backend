package com.flowboard.list.dto.response;
import com.flowboard.list.entity.TaskList;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ListResponse {
    private Long id; private Long boardId; private String name;
    private Integer position; private String color; private Boolean isArchived; private LocalDateTime createdAt;
    public static ListResponse from(TaskList l) {
        return ListResponse.builder().id(l.getId()).boardId(l.getBoardId()).name(l.getName())
            .position(l.getPosition()).color(l.getColor()).isArchived(l.getIsArchived())
            .createdAt(l.getCreatedAt()).build();
    }
}