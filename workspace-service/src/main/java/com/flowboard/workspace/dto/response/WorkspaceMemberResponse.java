package com.flowboard.workspace.dto.response;

import com.flowboard.workspace.entity.WorkspaceMember;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkspaceMemberResponse {
    private Long id;
    private Long userId;
    private Long workspaceId;
    private WorkspaceMember.Role role;
    private LocalDateTime joinedAt;

    public static WorkspaceMemberResponse from(WorkspaceMember m) {
        return WorkspaceMemberResponse.builder()
                .id(m.getId())
                .userId(m.getUserId())
                .workspaceId(m.getWorkspace().getId())
                .role(m.getRole())
                .joinedAt(m.getJoinedAt())
                .build();
    }
}
