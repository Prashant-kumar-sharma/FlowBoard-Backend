package com.flowboard.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BroadcastRequest {
    @NotEmpty
    private List<Long> recipientIds;
    
    @NotBlank
    private String title;
    
    @NotBlank
    private String message;
}
