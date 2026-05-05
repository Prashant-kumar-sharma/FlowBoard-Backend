package com.flowboard.card.dto.response;
import com.flowboard.card.entity.Card;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CardResponse {
    private Long id;
    private String title;
    private String description;
    private Long listId;
    private Long boardId;
    private Integer position;
    private Card.Priority priority;
    private Card.Status status;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Long assigneeId;
    private Long createdById;
    private Boolean isArchived;
    private String coverColor;
    private Boolean isOverdue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CardResponse from(Card c) {
        boolean overdue = c.getDueDate() != null
                && c.getDueDate().isBefore(LocalDate.now())
                && c.getStatus() != Card.Status.DONE;
        return CardResponse.builder()
                .id(c.getId()).title(c.getTitle()).description(c.getDescription())
                .listId(c.getListId()).boardId(c.getBoardId()).position(c.getPosition())
                .priority(c.getPriority()).status(c.getStatus()).dueDate(c.getDueDate())
                .startDate(c.getStartDate()).assigneeId(c.getAssigneeId())
                .createdById(c.getCreatedById()).isArchived(c.getIsArchived())
                .coverColor(c.getCoverColor()).isOverdue(overdue)
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }
}