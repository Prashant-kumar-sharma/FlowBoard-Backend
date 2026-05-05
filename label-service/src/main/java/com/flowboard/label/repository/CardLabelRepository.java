package com.flowboard.label.repository;
import com.flowboard.label.entity.CardLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CardLabelRepository extends JpaRepository<CardLabel, Long> {
    List<CardLabel> findByCardId(Long cardId);
    boolean existsByCardIdAndLabelId(Long cardId, Long labelId);
    void deleteByCardIdAndLabelId(Long cardId, Long labelId);
}