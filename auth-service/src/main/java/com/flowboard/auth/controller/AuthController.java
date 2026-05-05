package com.flowboard.auth.controller;

import com.flowboard.auth.dto.request.EmailOtpRequest;
import com.flowboard.auth.dto.request.LoginRequest;
import com.flowboard.auth.dto.request.RegisterRequest;
import com.flowboard.auth.dto.request.ResetPasswordOtpConfirmRequest;
import com.flowboard.auth.dto.request.ResetPasswordRequest;
import com.flowboard.auth.dto.request.UpdateProfileRequest;
import com.flowboard.auth.dto.request.VerifyOtpRequest;
import com.flowboard.auth.dto.response.AuthResponse;
import com.flowboard.auth.dto.response.OtpChallengeResponse;
import com.flowboard.auth.dto.response.UserResponse;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.exception.ResourceNotFoundException;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    @Operation(summary = "GUEST - Register new account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/register/request-otp")
    @Operation(summary = "GUEST - Request registration OTP")
    public ResponseEntity<OtpChallengeResponse> requestRegistrationOtp(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.requestRegistrationOtp(request));
    }

    @PostMapping("/register/verify-otp")
    @Operation(summary = "GUEST - Verify registration OTP and create account")
    public ResponseEntity<AuthResponse> verifyRegistrationOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.verifyRegistrationOtp(request));
    }

    @PostMapping("/login")
    @Operation(summary = "GUEST - Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/login/request-otp")
    @Operation(summary = "GUEST - Request sign-in OTP")
    public ResponseEntity<OtpChallengeResponse> requestLoginOtp(@Valid @RequestBody EmailOtpRequest request) {
        return ResponseEntity.ok(authService.requestLoginOtp(request));
    }

    @PostMapping("/login/verify-otp")
    @Operation(summary = "GUEST - Verify sign-in OTP")
    public ResponseEntity<AuthResponse> verifyLoginOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyLoginOtp(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "GUEST - Legacy reset endpoint, now blocked")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password/request-otp")
    @Operation(summary = "GUEST - Request password reset OTP")
    public ResponseEntity<OtpChallengeResponse> requestPasswordResetOtp(@Valid @RequestBody EmailOtpRequest request) {
        return ResponseEntity.ok(authService.requestPasswordResetOtp(request));
    }

    @PostMapping("/reset-password/confirm")
    @Operation(summary = "GUEST - Confirm password reset with OTP")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ResetPasswordOtpConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "MEMBER - Logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {
        authService.logout(getUserId(userDetails), extractToken(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "MEMBER - Exchange refresh token for new access token")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refreshToken(body.get("refreshToken")));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "MEMBER - Get own profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getProfile(getUserId(userDetails)));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "MEMBER - Update own profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(getUserId(userDetails), request));
    }

    @PutMapping("/password")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "MEMBER - Change password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        authService.changePassword(getUserId(userDetails), body.get("oldPassword"), body.get("newPassword"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "MEMBER - Search users to invite")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(authService.searchUsers(q));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "PLATFORM_ADMIN - List all users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "MEMBER - Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PatchMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "PLATFORM_ADMIN - Suspend user")
    public ResponseEntity<Void> deactivate(@PathVariable Long userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/reactivate")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "PLATFORM_ADMIN - Restore user")
    public ResponseEntity<Void> reactivate(@PathVariable Long userId) {
        authService.reactivateAccount(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "PLATFORM_ADMIN - Permanently delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return "";
    }
}
