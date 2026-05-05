package com.flowboard.card.dto.request;
import com.flowboard.card.entity.Card;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateCardRequest {
    @NotBlank private String title;
    private String description;
    private Long listId;
    private Long boardId;
    private Card.Priority priority = Card.Priority.MEDIUM;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Long assigneeId;
    private String coverColor = "#FFFFFF";
}