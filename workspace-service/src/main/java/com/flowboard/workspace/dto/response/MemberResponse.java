package com.flowboard.workspace.dto.response;
import com.flowboard.workspace.entity.WorkspaceMember;
import lombok.*;
import java.time.LocalDateTime;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MemberResponse {
    private Long userId;
    private String role;
    private LocalDateTime joinedAt;
    public static MemberResponse from(WorkspaceMember m) {
        return MemberResponse.builder()
            .userId(m.getUserId()).role(m.getRole().name()).joinedAt(m.getJoinedAt()).build();
    }
}