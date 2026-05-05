package com.flowboard.board.dto.request;
import com.flowboard.board.entity.Board;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBoardRequest {
    @NotBlank private String name;
    private String description;
    private String background = "#0079BF";
    private Board.Visibility visibility = Board.Visibility.PRIVATE;
    private Long workspaceId;
}