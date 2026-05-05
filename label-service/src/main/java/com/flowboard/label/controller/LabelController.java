package com.flowboard.label.controller;

import com.flowboard.label.dto.response.ChecklistResponse;
import com.flowboard.label.entity.*;
import com.flowboard.label.service.LabelService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j @RestController @RequestMapping("/api/v1")
@RequiredArgsConstructor @Tag(name = "Labels & Checklists") @SecurityRequirement(name = "bearerAuth")
public class LabelController {
    private final LabelService labelService;

    @PostMapping("/boards/{boardId}/labels")
    public ResponseEntity<Label> createLabel(@PathVariable Long boardId, @RequestBody com.flowboard.label.dto.request.CreateLabelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labelService.createLabel(boardId, request.getName(), request.getColor()));
    }

    @GetMapping("/boards/{boardId}/labels")
    public ResponseEntity<List<Label>> getLabelsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(labelService.getLabelsByBoard(boardId));
    }

    @PutMapping("/labels/{id}")
    public ResponseEntity<Label> updateLabel(@PathVariable Long id, @RequestBody com.flowboard.label.dto.request.CreateLabelRequest request) {
        return ResponseEntity.ok(labelService.updateLabel(id, request.getName(), request.getColor()));
    }

    @DeleteMapping("/labels/{id}")
    public ResponseEntity<Void> deleteLabel(@PathVariable Long id) {
        labelService.deleteLabel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cards/{cardId}/labels/{labelId}")
    public ResponseEntity<CardLabel> addToCard(@PathVariable Long cardId, @PathVariable Long labelId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labelService.addLabelToCard(cardId, labelId));
    }

    @DeleteMapping("/cards/{cardId}/labels/{labelId}")
    public ResponseEntity<Void> removeFromCard(@PathVariable Long cardId, @PathVariable Long labelId) {
        labelService.removeLabelFromCard(cardId, labelId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cards/{cardId}/labels")
    public ResponseEntity<List<Label>> getForCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(labelService.getLabelsForCard(cardId));
    }

    @PostMapping("/cards/{cardId}/checklists")
    public ResponseEntity<ChecklistResponse> createChecklist(@PathVariable Long cardId, @RequestBody com.flowboard.label.dto.request.CreateChecklistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ChecklistResponse.from(labelService.createChecklist(cardId, request.getTitle())));
    }

    @GetMapping("/cards/{cardId}/checklists")
    public ResponseEntity<List<ChecklistResponse>> getChecklists(@PathVariable Long cardId) {
        return ResponseEntity.ok(labelService.getChecklistsByCard(cardId).stream().map(ChecklistResponse::from).toList());
    }

    @PostMapping("/checklists/{checklistId}/items")
    public ResponseEntity<ChecklistResponse.ItemResponse> addItem(@PathVariable Long checklistId, @RequestBody com.flowboard.label.dto.request.CreateChecklistItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ChecklistResponse.ItemResponse.from(labelService.addItem(checklistId, request.getText(), request.getAssigneeId())));
    }

    @PatchMapping("/checklist-items/{itemId}/toggle")
    public ResponseEntity<ChecklistResponse.ItemResponse> toggle(@PathVariable Long itemId) {
        return ResponseEntity.ok(ChecklistResponse.ItemResponse.from(labelService.toggleItem(itemId)));
    }

    @GetMapping("/checklists/{checklistId}/progress")
    public ResponseEntity<Map<String,Integer>> progress(@PathVariable Long checklistId) {
        return ResponseEntity.ok(Map.of("progress", labelService.getChecklistProgress(checklistId)));
    }

    @DeleteMapping("/checklists/{id}")
    public ResponseEntity<Void> deleteChecklist(@PathVariable Long id) {
        labelService.deleteChecklist(id);
        return ResponseEntity.noContent().build();
    }
}
