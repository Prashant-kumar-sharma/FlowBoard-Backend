package com.flowboard.list.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class CreateListRequest {
    @NotBlank private String name;
    private Long boardId;
    private String color;
}