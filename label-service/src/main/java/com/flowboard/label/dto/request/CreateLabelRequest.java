package com.flowboard.label.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLabelRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String color;
}
