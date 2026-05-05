package com.flowboard.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAttachmentRequest {
    @NotBlank
    private String fileName;
    
    @NotBlank
    private String fileUrl;
    
    private String fileType;
    
    private Long sizeKb = 0L;
}
