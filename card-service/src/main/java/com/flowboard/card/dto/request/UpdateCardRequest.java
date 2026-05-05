package com.flowboard.card.dto.request;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateCardRequest {
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDate dueDate;
    private LocalDate startDate;
    private Long assigneeId;
    private String coverColor;
}