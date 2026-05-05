package com.flowboard.label.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "card_labels",
    uniqueConstraints = @UniqueConstraint(columnNames = {"card_id","label_id"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CardLabel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long cardId;
    private Long labelId;
}