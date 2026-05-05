package com.flowboard.board.dto.request;

import com.flowboard.board.entity.BoardMember;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBoardMemberRoleRequest {
    @NotNull
    private BoardMember.Role role;
}
