package com.flowboard.label.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateChecklistItemRequest {
    @NotBlank
    private String text;
    
    private Long assigneeId;
}
