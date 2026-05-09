package com.flowboard.auth.service;

import com.flowboard.auth.dto.request.LoginRequest;
import com.flowboard.auth.dto.request.RegisterRequest;
import com.flowboard.auth.dto.request.EmailOtpRequest;
import com.flowboard.auth.dto.request.ResetPasswordOtpConfirmRequest;
import com.flowboard.auth.dto.request.ResetPasswordRequest;
import com.flowboard.auth.dto.request.UpdateProfileRequest;
import com.flowboard.auth.dto.request.VerifyOtpRequest;
import com.flowboard.auth.dto.response.AuthResponse;
import com.flowboard.auth.dto.response.OtpChallengeResponse;
import com.flowboard.auth.dto.response.UserResponse;
import java.util.List;

public interface AuthService {
    // GUEST
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    OtpChallengeResponse requestRegistrationOtp(RegisterRequest request);
    AuthResponse verifyRegistrationOtp(VerifyOtpRequest request);
    OtpChallengeResponse requestLoginOtp(LoginRequest request);
    AuthResponse verifyLoginOtp(VerifyOtpRequest request);
    OtpChallengeResponse requestPasswordResetOtp(EmailOtpRequest request);
    void confirmPasswordReset(ResetPasswordOtpConfirmRequest request);
    void resetPassword(ResetPasswordRequest request);
    // MEMBER
    void logout(Long userId, String accessToken);
    AuthResponse refreshToken(String refreshToken);
    UserResponse getProfile(Long userId);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, String oldPassword, String newPassword);
    // PLATFORM_ADMIN
    void deactivateAccount(Long userId);
    void deactivateAccount(Long actorUserId, Long userId);
    void reactivateAccount(Long userId);
    void deleteUser(Long userId);
    void deleteUser(Long actorUserId, Long userId);
    UserResponse changeRole(Long userId, String newRole);
    UserResponse changeRole(Long actorUserId, Long userId, String newRole);
    List<UserResponse> searchUsers(String query);
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Long userId);
    UserResponse getUserByUsername(String username);
}
