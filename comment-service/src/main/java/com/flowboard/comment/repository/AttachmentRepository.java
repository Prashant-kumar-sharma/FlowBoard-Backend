package com.flowboard.comment.repository;
import com.flowboard.comment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByCardId(Long cardId);
}