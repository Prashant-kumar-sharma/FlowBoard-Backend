package com.flowboard.auth.controller;

import com.flowboard.auth.dto.response.UserResponse;
import com.flowboard.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API for inter-service communication.
 * These endpoints are NOT secured — they should only be accessible
 * within the cluster (not exposed through the API Gateway to the public).
 */
@RestController
@RequestMapping("/api/v1/auth/internal")
@RequiredArgsConstructor
@Tag(name = "Internal (Service-to-Service)")
public class InternalUserController {

    private final AuthService authService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "INTERNAL — Get user by ID (no auth required)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @GetMapping("/users/username/{username}")
    @Operation(summary = "INTERNAL — Get user by username (no auth required)")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(authService.getUserByUsername(username));
    }
}
