package com.flowboard.board.dto.response;

import com.flowboard.board.entity.BoardAuditEvent;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardAuditEventResponse {
    private Long id;
    private Long boardId;
    private Long actorId;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private LocalDateTime createdAt;

    public static BoardAuditEventResponse from(BoardAuditEvent event) {
        return BoardAuditEventResponse.builder()
                .id(event.getId())
                .boardId(event.getBoardId())
                .actorId(event.getActorId())
                .action(event.getAction())
                .targetType(event.getTargetType())
                .targetId(event.getTargetId())
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
