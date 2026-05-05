package com.flowboard.board.dto.request;

import com.flowboard.board.entity.BoardMember;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddBoardMemberRequest {
    @NotNull
    private Long userId;
    
    private BoardMember.Role role = BoardMember.Role.MEMBER;
}
