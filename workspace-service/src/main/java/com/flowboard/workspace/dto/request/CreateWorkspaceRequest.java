package com.flowboard.workspace.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CreateWorkspaceRequest {
    @NotBlank private String name;
    private String description;
    private String logoUrl;
    private String visibility = "PRIVATE";
}