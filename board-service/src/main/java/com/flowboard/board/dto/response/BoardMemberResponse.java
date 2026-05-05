package com.flowboard.board.dto.response;
import com.flowboard.board.entity.BoardMember;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BoardMemberResponse {
    private Long id;
    private Long userId;
    private Long boardId;
    private BoardMember.Role role;
    private LocalDateTime addedAt;

    public static BoardMemberResponse from(BoardMember m) {
        return BoardMemberResponse.builder()
                .id(m.getId()).userId(m.getUserId())
                .boardId(m.getBoard().getId()).role(m.getRole()).addedAt(m.getAddedAt()).build();
    }
}