package com.flowboard.workspace.dto.response;

import com.flowboard.workspace.entity.WorkspaceAuditEvent;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceAuditEventResponse {
    private Long id;
    private Long workspaceId;
    private Long actorId;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private LocalDateTime createdAt;

    public static WorkspaceAuditEventResponse from(WorkspaceAuditEvent event) {
        return WorkspaceAuditEventResponse.builder()
                .id(event.getId())
                .workspaceId(event.getWorkspaceId())
                .actorId(event.getActorId())
                .action(event.getAction())
                .targetType(event.getTargetType())
                .targetId(event.getTargetId())
                .details(event.getDetails())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
