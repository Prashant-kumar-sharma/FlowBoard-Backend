package com.flowboard.auth.controller;

import com.flowboard.auth.dto.response.UserResponse;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.exception.ResourceNotFoundException;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PLATFORM_ADMIN actor — full system management panel.
 * All endpoints require ROLE_PLATFORM_ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Admin Panel", description = "PLATFORM_ADMIN — full system management")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping("/users")
    @Operation(summary = "List all users on the platform")
    public ResponseEntity<List<UserResponse>> listAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Promote or demote a user's platform role")
    public ResponseEntity<UserResponse> changeRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        // role values: MEMBER | BOARD_ADMIN | PLATFORM_ADMIN
        return ResponseEntity.ok(authService.changeRole(getUserId(userDetails), userId, body.get("role")));
    }

    @PatchMapping("/users/{userId}/suspend")
    @Operation(summary = "Suspend (deactivate) a user account")
    public ResponseEntity<Void> suspend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId) {
        authService.deactivateAccount(getUserId(userDetails), userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/restore")
    @Operation(summary = "Restore a suspended user account")
    public ResponseEntity<Void> restore(@PathVariable Long userId) {
        authService.reactivateAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Permanently delete a user account")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long userId) {
        authService.deleteUser(getUserId(userDetails), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform-wide stats (total users, active, admins)")
    public ResponseEntity<Map<String, Object>> stats() {
        List<UserResponse> all = authService.getAllUsers();
        long active  = all.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        long admins  = all.stream().filter(u -> "PLATFORM_ADMIN".equals(u.getRole() != null ? u.getRole().name() : "")).count();
        return ResponseEntity.ok(Map.of(
                "totalUsers", all.size(),
                "activeUsers", active,
                "platformAdmins", admins
        ));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
