package com.flowboard.comment.service.impl;

import com.flowboard.comment.entity.Attachment;
import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.exception.AccessDeniedException;
import com.flowboard.comment.exception.ResourceNotFoundException;
import com.flowboard.comment.kafka.CommentEventProducer;
import com.flowboard.comment.repository.AttachmentRepository;
import com.flowboard.comment.repository.CommentRepository;
import com.flowboard.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j @Service @RequiredArgsConstructor @Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final CommentEventProducer commentEventProducer;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public Comment addComment(Long cardId, Long authorId, String content, Long parentCommentId) {
        String trimmedContent = normalizeContent(content);
        Comment comment = Comment.builder()
                .cardId(cardId).authorId(authorId).content(trimmedContent)
                .parentCommentId(parentCommentId).build();
        Comment saved = commentRepository.save(comment);
        commentEventProducer.sendCommentAdded(saved.getId(), cardId, authorId, trimmedContent);
        messagingTemplate.convertAndSend("/topic/board/card-" + cardId + "/comment-added", saved);
        return saved;
    }

    @Override @Transactional(readOnly = true)
    public Comment getById(Long id) {
        return commentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + id));
    }

    @Override @Transactional(readOnly = true)
    public List<Comment> getByCard(Long cardId) {
        return commentRepository.findByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(cardId);
    }

    @Override @Transactional(readOnly = true)
    public List<Comment> getReplies(Long commentId) {
        return commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
    }

    @Override
    public Comment update(Long id, String content, Long requesterId) {
        Comment comment = findCommentById(id);
        if (!comment.getAuthorId().equals(requesterId)) throw new AccessDeniedException("Can only edit own comments");
        comment.setContent(normalizeContent(content));
        return commentRepository.save(comment);
    }

    @Override
    public void delete(Long id, Long requesterId) {
        Comment comment = findCommentById(id);
        if (!comment.getAuthorId().equals(requesterId)) throw new AccessDeniedException("Can only delete own comments");
        comment.setIsDeleted(true);
        comment.setContent("[deleted]");
        commentRepository.save(comment);
    }

    @Override
    public Attachment addAttachment(Long cardId, Long uploaderId, String fileName, String fileUrl, String fileType, Long sizeKb) {
        return attachmentRepository.save(Attachment.builder()
                .cardId(cardId).uploaderId(uploaderId).fileName(fileName)
                .fileUrl(fileUrl).fileType(fileType).sizeKb(sizeKb).build());
    }

    @Override @Transactional(readOnly = true)
    public List<Attachment> getAttachments(Long cardId) { return attachmentRepository.findByCardId(cardId); }

    @Override
    public void deleteAttachment(Long id, Long requesterId) {
        Attachment att = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        if (!att.getUploaderId().equals(requesterId)) throw new AccessDeniedException("Can only delete own attachments");
        attachmentRepository.delete(att);
    }

    @Override @Transactional(readOnly = true)
    public long getCommentCount(Long cardId) { return commentRepository.countByCardId(cardId); }

    private Comment findCommentById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + id));
    }

    private String normalizeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content is required");
        }
        return content.trim();
    }
}
