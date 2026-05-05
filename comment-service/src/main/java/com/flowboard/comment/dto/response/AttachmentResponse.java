package com.flowboard.comment.dto.response;
import com.flowboard.comment.entity.Attachment;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttachmentResponse {
    private Long id; private Long cardId; private Long uploaderId;
    private String fileName; private String fileUrl; private String fileType;
    private Long sizeKb; private LocalDateTime uploadedAt;
    public static AttachmentResponse from(Attachment a) {
        return AttachmentResponse.builder().id(a.getId()).cardId(a.getCardId()).uploaderId(a.getUploaderId())
            .fileName(a.getFileName()).fileUrl(a.getFileUrl()).fileType(a.getFileType())
            .sizeKb(a.getSizeKb()).uploadedAt(a.getUploadedAt()).build();
    }
}