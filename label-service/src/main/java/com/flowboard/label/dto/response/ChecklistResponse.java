package com.flowboard.label.dto.response;
import com.flowboard.label.entity.Checklist;
import com.flowboard.label.entity.ChecklistItem;
import lombok.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChecklistResponse {
    private Long id; private Long cardId; private String title; private Integer position;
    private List<ItemResponse> items; private Integer completionPercent;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ItemResponse {
        private Long id; private Long checklistId; private String text; private Boolean isCompleted;
        private Long assigneeId; private LocalDate dueDate; private Integer position;
        public static ItemResponse from(ChecklistItem i) {
            return ItemResponse.builder()
                .id(i.getId())
                .checklistId(i.getChecklist() != null ? i.getChecklist().getId() : null)
                .text(i.getText())
                .isCompleted(i.getIsCompleted())
                .assigneeId(i.getAssigneeId())
                .dueDate(i.getDueDate())
                .position(i.getPosition())
                .build();
        }
    }

    public static ChecklistResponse from(Checklist c) {
        List<ItemResponse> items = c.getItems().stream().map(ItemResponse::from).collect(Collectors.toList());
        int total = items.size();
        int done = (int) items.stream().filter(ItemResponse::getIsCompleted).count();
        int pct = total == 0 ? 0 : (done * 100 / total);
        return ChecklistResponse.builder().id(c.getId()).cardId(c.getCardId()).title(c.getTitle())
            .position(c.getPosition()).items(items).completionPercent(pct).build();
    }
}
