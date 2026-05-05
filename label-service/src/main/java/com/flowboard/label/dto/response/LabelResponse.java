package com.flowboard.label.dto.response;
import com.flowboard.label.entity.Label;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LabelResponse {
    private Long id; private Long boardId; private String name; private String color;
    public static LabelResponse from(Label l) {
        return LabelResponse.builder().id(l.getId()).boardId(l.getBoardId()).name(l.getName()).color(l.getColor()).build();
    }
}