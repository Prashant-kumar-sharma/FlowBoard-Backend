package com.flowboard.board.dto.response;
import com.flowboard.board.entity.Board;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BoardResponse {
    private Long id;
    private String name;
    private String description;
    private Long workspaceId;
    private Long createdById;
    private String background;
    private Board.Visibility visibility;
    private Boolean isClosed;
    private List<BoardMemberResponse> members;
    private LocalDateTime createdAt;

    public static BoardResponse from(Board b) {
        return BoardResponse.builder()
                .id(b.getId()).name(b.getName()).description(b.getDescription())
                .workspaceId(b.getWorkspaceId()).createdById(b.getCreatedById())
                .background(b.getBackground()).visibility(b.getVisibility())
                .isClosed(b.getIsClosed()).createdAt(b.getCreatedAt()).build();
    }
}