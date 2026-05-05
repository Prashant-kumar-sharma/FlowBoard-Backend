package com.flowboard.list.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveListRequest {
    @NotNull
    private Long targetBoardId;
}
