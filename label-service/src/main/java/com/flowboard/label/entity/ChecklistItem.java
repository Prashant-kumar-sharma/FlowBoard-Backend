package com.flowboard.label.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "checklist_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ChecklistItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "checklist_id") private Checklist checklist;
    @Column(nullable = false) private String text;
    @Builder.Default private Boolean isCompleted = false;
    private Long assigneeId;
    private LocalDate dueDate;
    @Builder.Default @Column(nullable = false) private Integer position = 0;
}
