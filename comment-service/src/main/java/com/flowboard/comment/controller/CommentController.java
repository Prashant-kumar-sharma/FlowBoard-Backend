package com.flowboard.comment.controller;

import com.flowboard.comment.entity.Attachment;
import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j @RestController @RequestMapping("/api/v1")
@RequiredArgsConstructor @Tag(name = "Comments & Attachments") @SecurityRequirement(name = "bearerAuth")
public class CommentController {
    private final CommentService commentService;
    private final com.flowboard.comment.service.FileStorageService fileStorageService;

    @PostMapping("/cards/{cardId}/comments")
    @Operation(summary = "Add a comment to a card")
    public ResponseEntity<Comment> addComment(@PathVariable Long cardId, @RequestBody com.flowboard.comment.dto.request.CreateCommentRequest request,
                                               @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(cardId, userId, request.getContent(), request.getParentCommentId()));
    }

    @GetMapping("/cards/{cardId}/comments")
    @Operation(summary = "Get top-level comments for a card")
    public ResponseEntity<List<Comment>> getByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getByCard(cardId));
    }

    @GetMapping("/comments/{id}/replies")
    @Operation(summary = "Get replies to a comment")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getReplies(id));
    }

    @PutMapping("/comments/{id}")
    @Operation(summary = "Edit a comment")
    public ResponseEntity<Comment> update(@PathVariable Long id, @RequestBody com.flowboard.comment.dto.request.CreateCommentRequest request,
                                           @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(commentService.update(id, request.getContent(), userId));
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        commentService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cards/{cardId}/comments/count")
    public ResponseEntity<Map<String,Long>> count(@PathVariable Long cardId) {
        return ResponseEntity.ok(Map.of("count", commentService.getCommentCount(cardId)));
    }

    @PostMapping("/cards/{cardId}/attachments")
    @Operation(summary = "Add an attachment to a card")
    public ResponseEntity<Attachment> addAttachment(@PathVariable Long cardId, @RequestBody com.flowboard.comment.dto.request.CreateAttachmentRequest req,
                                                     @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addAttachment(cardId, userId, req.getFileName(), req.getFileUrl(), req.getFileType(), req.getSizeKb()));
    }

    @GetMapping("/cards/{cardId}/attachments")
    @Operation(summary = "Get attachments for a card")
    public ResponseEntity<List<Attachment>> getAttachments(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getAttachments(cardId));
    }

    @DeleteMapping("/attachments/{id}")
    @Operation(summary = "Delete an attachment")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        commentService.deleteAttachment(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cards/{cardId}/attachments/upload")
    @Operation(summary = "Upload a physical file and create attachment metadata")
    public ResponseEntity<Attachment> uploadFile(@PathVariable Long cardId, 
                                                 @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                 @RequestHeader("X-User-Id") Long userId) {
        String filename = fileStorageService.store(file);
        String url = "/api/v1/files/" + filename;
        String type = file.getContentType();
        long size = file.getSize() / 1024;
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addAttachment(cardId, userId, file.getOriginalFilename(), url, type, size));
    }

    @GetMapping("/files/{filename:.+}")
    @Operation(summary = "Retrieve an uploaded file")
    public ResponseEntity<org.springframework.core.io.Resource> getFile(@PathVariable String filename) {
        try {
            java.nio.file.Path path = fileStorageService.load(filename);
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
            String contentType = java.nio.file.Files.probeContentType(path);
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
