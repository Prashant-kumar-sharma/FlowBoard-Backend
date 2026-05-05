package com.flowboard.comment.dto.response;
import com.flowboard.comment.entity.Comment;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CommentResponse {
    private Long id; private Long cardId; private Long authorId; private String content;
    private Long parentCommentId; private Boolean isDeleted; private LocalDateTime createdAt;
    private List<CommentResponse> replies;
    public static CommentResponse from(Comment c) {
        return CommentResponse.builder().id(c.getId()).cardId(c.getCardId()).authorId(c.getAuthorId())
            .content(c.getIsDeleted() ? "[deleted]" : c.getContent()).parentCommentId(c.getParentCommentId())
            .isDeleted(c.getIsDeleted()).createdAt(c.getCreatedAt()).build();
    }
}