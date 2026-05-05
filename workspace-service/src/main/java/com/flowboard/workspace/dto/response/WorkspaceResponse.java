package com.flowboard.workspace.dto.response;
import com.flowboard.workspace.entity.Workspace;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String visibility;
    private String logoUrl;
    private List<MemberResponse> members;
    private LocalDateTime createdAt;
    public static WorkspaceResponse from(Workspace w) {
        return WorkspaceResponse.builder()
            .id(w.getId()).name(w.getName()).description(w.getDescription())
            .ownerId(w.getOwnerId()).visibility(w.getVisibility().name())
            .logoUrl(w.getLogoUrl()).createdAt(w.getCreatedAt()).build();
    }
}