package com.flowboard.card.dto.request;
import lombok.Data;
@Data
public class MoveCardRequest {
    private Long targetListId;
    private Integer position;
}
