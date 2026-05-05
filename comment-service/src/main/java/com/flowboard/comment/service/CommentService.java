package com.flowboard.comment.service;
import com.flowboard.comment.entity.Attachment;
import com.flowboard.comment.entity.Comment;
import java.util.List;
public interface CommentService {
    Comment addComment(Long cardId, Long authorId, String content, Long parentCommentId);
    Comment getById(Long id);
    List<Comment> getByCard(Long cardId);
    List<Comment> getReplies(Long commentId);
    Comment update(Long id, String content, Long requesterId);
    void delete(Long id, Long requesterId);
    Attachment addAttachment(Long cardId, Long uploaderId, String fileName, String fileUrl, String fileType, Long sizeKb);
    List<Attachment> getAttachments(Long cardId);
    void deleteAttachment(Long id, Long requesterId);
    long getCommentCount(Long cardId);
}