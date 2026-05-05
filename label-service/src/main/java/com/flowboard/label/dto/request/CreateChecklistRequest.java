package com.flowboard.label.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateChecklistRequest {
    @NotBlank
    private String title;
}
