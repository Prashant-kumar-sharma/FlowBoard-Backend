package com.flowboard.card.repository;
import com.flowboard.card.entity.CardActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CardActivityRepository extends JpaRepository<CardActivity, Long> {
    List<CardActivity> findByCardIdOrderByCreatedAtDesc(Long cardId);
    List<CardActivity> findAllByOrderByCreatedAtDesc();
    void deleteByCardId(Long cardId);
    void deleteByCardIdIn(List<Long> cardIds);
}
