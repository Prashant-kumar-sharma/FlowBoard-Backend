package com.flowboard.workspace.dto.request;

import com.flowboard.workspace.entity.Workspace;
import lombok.Data;

@Data
public class UpdateWorkspaceRequest {
    private String name;
    private String description;
    private String logoUrl;
    private Workspace.Visibility visibility;
}
