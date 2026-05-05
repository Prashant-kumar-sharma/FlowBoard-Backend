package com.flowboard.comment.repository;
import com.flowboard.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByCardIdAndParentCommentIdIsNullOrderByCreatedAtAsc(Long cardId);
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(Long parentId);
    long countByCardId(Long cardId);
}