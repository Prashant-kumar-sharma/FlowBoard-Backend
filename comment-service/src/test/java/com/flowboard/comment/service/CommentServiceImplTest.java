package com.flowboard.comment.service;

import com.flowboard.comment.entity.Attachment;
import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.exception.AccessDeniedException;
import com.flowboard.comment.exception.ResourceNotFoundException;
import com.flowboard.comment.kafka.CommentEventProducer;
import com.flowboard.comment.repository.AttachmentRepository;
import com.flowboard.comment.repository.CommentRepository;
import com.flowboard.comment.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private CommentEventProducer commentEventProducer;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment comment;
    private Attachment attachment;

    @BeforeEach
    void setUp() {
        comment = Comment.builder()
                .id(1L)
                .cardId(100L)
                .authorId(7L)
                .content("hello")
                .isDeleted(false)
                .build();
        attachment = Attachment.builder()
                .id(2L)
                .cardId(100L)
                .uploaderId(7L)
                .fileName("a.txt")
                .build();
    }

    @Test
    void addCommentNormalizesAndBroadcasts() {
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        Comment saved = commentService.addComment(100L, 7L, "  hello  ", null);

        assertThat(saved.getContent()).isEqualTo("hello");
        verify(commentEventProducer).sendCommentAdded(1L, 100L, 7L, "hello");
        verify(messagingTemplate).convertAndSend("/topic/board/card-100/comment-added", comment);
    }

    @Test
    void addCommentRejectsBlankContent() {
        assertThatThrownBy(() -> commentService.addComment(100L, 7L, "   ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getByIdReturnsComment() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThat(commentService.getById(1L)).isEqualTo(comment);
    }

    @Test
    void getByCardAndRepliesReturnRepositoryResults() {
        when(commentRepository.findByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(100L)).thenReturn(List.of(comment));
        when(commentRepository.findByParentCommentIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(comment));

        assertThat(commentService.getByCard(100L)).containsExactly(comment);
        assertThat(commentService.getReplies(1L)).containsExactly(comment);
    }

    @Test
    void updateRequiresAuthor() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(1L, "updated", 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateTrimsAndSavesForAuthor() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        Comment updated = commentService.update(1L, "  updated  ", 7L);

        assertThat(updated.getContent()).isEqualTo("updated");
    }

    @Test
    void deleteMarksCommentDeleted() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        commentService.delete(1L, 7L);

        assertThat(comment.getIsDeleted()).isTrue();
        assertThat(comment.getContent()).isEqualTo("[deleted]");
    }

    @Test
    void deleteAttachmentRequiresUploader() {
        when(attachmentRepository.findById(2L)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> commentService.deleteAttachment(2L, 88L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteAttachmentThrowsWhenMissing() {
        when(attachmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteAttachment(2L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAttachmentsAndCountsDelegateToRepository() {
        when(attachmentRepository.findByCardId(100L)).thenReturn(List.of(attachment));
        when(commentRepository.countByCardId(100L)).thenReturn(3L);

        assertThat(commentService.getAttachments(100L)).containsExactly(attachment);
        assertThat(commentService.getCommentCount(100L)).isEqualTo(3L);
    }

    @Test
    void addAttachmentBuildsAndSavesAttachment() {
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(attachment);

        Attachment saved = commentService.addAttachment(100L, 7L, "a.txt", "/a.txt", "text/plain", 12L);

        assertThat(saved.getUploaderId()).isEqualTo(7L);
        assertThat(saved.getCardId()).isEqualTo(100L);
    }
}
