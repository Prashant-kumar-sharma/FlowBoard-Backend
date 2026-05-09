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
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    private AuthController controller;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, userRepository);
        userDetails = org.springframework.security.core.userdetails.User.withUsername("alice@test.com")
                .password("pw")
                .roles("MEMBER")
                .build();

        lenient().when(userRepository.findByEmail("alice@test.com"))
                .thenReturn(Optional.of(User.builder().id(1L).email("alice@test.com").build()));
    }

    @Test
    void registerReturnsCreatedResponse() {
        AuthResponse response = AuthResponse.builder().accessToken("token").build();
        when(authService.register(org.mockito.ArgumentMatchers.any(RegisterRequest.class))).thenReturn(response);

        assertThat(controller.register(new RegisterRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void loginReturnsOkResponse() {
        AuthResponse response = AuthResponse.builder().accessToken("token").build();
        when(authService.login(org.mockito.ArgumentMatchers.any(LoginRequest.class))).thenReturn(response);

        assertThat(controller.login(new LoginRequest()).getBody()).isEqualTo(response);
    }

    @Test
    void requestRegistrationOtpReturnsOk() {
        OtpChallengeResponse response = OtpChallengeResponse.builder().message("sent").expiresInSeconds(600).build();
        when(authService.requestRegistrationOtp(org.mockito.ArgumentMatchers.any(RegisterRequest.class))).thenReturn(response);

        assertThat(controller.requestRegistrationOtp(new RegisterRequest()).getBody()).isEqualTo(response);
    }

    @Test
    void verifyRegistrationOtpReturnsCreated() {
        AuthResponse response = AuthResponse.builder().accessToken("token").build();
        when(authService.verifyRegistrationOtp(org.mockito.ArgumentMatchers.any(VerifyOtpRequest.class))).thenReturn(response);

        assertThat(controller.verifyRegistrationOtp(new VerifyOtpRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void requestLoginOtpReturnsOk() {
        OtpChallengeResponse response = OtpChallengeResponse.builder().message("sent").expiresInSeconds(600).build();
        when(authService.requestLoginOtp(org.mockito.ArgumentMatchers.any(LoginRequest.class))).thenReturn(response);

        assertThat(controller.requestLoginOtp(new LoginRequest()).getBody()).isEqualTo(response);
    }

    @Test
    void verifyLoginOtpReturnsOk() {
        AuthResponse response = AuthResponse.builder().accessToken("token").build();
        when(authService.verifyLoginOtp(org.mockito.ArgumentMatchers.any(VerifyOtpRequest.class))).thenReturn(response);

        assertThat(controller.verifyLoginOtp(new VerifyOtpRequest()).getBody()).isEqualTo(response);
    }

    @Test
    void resetPasswordDelegatesToService() {
        ResetPasswordRequest requestBody = new ResetPasswordRequest();
        requestBody.setEmail("alice@test.com");
        requestBody.setNewPassword("NewPassword1!");

        assertThat(controller.resetPassword(requestBody).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).resetPassword(requestBody);
    }

    @Test
    void requestPasswordResetOtpReturnsOk() {
        OtpChallengeResponse response = OtpChallengeResponse.builder().message("sent").expiresInSeconds(600).build();
        when(authService.requestPasswordResetOtp(org.mockito.ArgumentMatchers.any(EmailOtpRequest.class))).thenReturn(response);

        assertThat(controller.requestPasswordResetOtp(new EmailOtpRequest()).getBody()).isEqualTo(response);
    }

    @Test
    void confirmPasswordResetDelegatesToService() {
        ResetPasswordOtpConfirmRequest requestBody = new ResetPasswordOtpConfirmRequest();

        assertThat(controller.confirmPasswordReset(requestBody).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).confirmPasswordReset(requestBody);
    }

    @Test
    void logoutPassesBearerTokenToService() {
        when(request.getHeader("Authorization")).thenReturn("Bearer abc");

        assertThat(controller.logout(null, userDetails, request).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(authService).logout(1L, "abc");
    }

    @Test
    void logoutUsesEmptyTokenWhenHeaderIsMissing() {
        when(request.getHeader("Authorization")).thenReturn(null);

        controller.logout(null, userDetails, request);

        verify(authService).logout(1L, "");
    }

    @Test
    void refreshDelegatesToService() {
        AuthResponse response = AuthResponse.builder().accessToken("new-token").build();
        when(authService.refreshToken("refresh")).thenReturn(response);

        assertThat(controller.refresh(Map.of("refreshToken", "refresh")).getBody()).isEqualTo(response);
    }

    @Test
    void getProfileDelegatesUsingResolvedUserId() {
        UserResponse response = UserResponse.builder().id(1L).email("alice@test.com").build();
        when(authService.getProfile(1L)).thenReturn(response);

        assertThat(controller.getProfile(null, userDetails).getBody()).isEqualTo(response);
    }

    @Test
    void updateProfileDelegatesUsingResolvedUserId() {
        UpdateProfileRequest requestBody = new UpdateProfileRequest();
        UserResponse response = UserResponse.builder().id(1L).username("alice").build();
        when(authService.updateProfile(1L, requestBody)).thenReturn(response);

        assertThat(controller.updateProfile(null, userDetails, requestBody).getBody()).isEqualTo(response);
    }

    @Test
    void changePasswordDelegatesToService() {
        assertThat(controller.changePassword(null, userDetails, Map.of("oldPassword", "old", "newPassword", "new"))
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(authService).changePassword(1L, "old", "new");
    }

    @Test
    void searchUsersDelegatesToService() {
        List<UserResponse> response = List.of(UserResponse.builder().username("alice").build());
        when(authService.searchUsers("ali")).thenReturn(response);

        assertThat(controller.searchUsers("ali").getBody()).isEqualTo(response);
    }

    @Test
    void getAllUsersDelegatesToService() {
        List<UserResponse> response = List.of(UserResponse.builder().username("alice").build());
        when(authService.getAllUsers()).thenReturn(response);

        assertThat(controller.getAllUsers().getBody()).isEqualTo(response);
    }

    @Test
    void getUserByIdDelegatesToService() {
        UserResponse response = UserResponse.builder().id(10L).build();
        when(authService.getUserById(10L)).thenReturn(response);

        assertThat(controller.getUserById(10L).getBody()).isEqualTo(response);
    }

    @Test
    void deactivateReactivateAndDeleteDelegateToService() {
        assertThat(controller.deactivate(7L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.reactivate(7L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteUser(7L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(authService).deactivateAccount(7L);
        verify(authService).reactivateAccount(7L);
        verify(authService).deleteUser(7L);
    }

    @Test
    void protectedEndpointsThrowWhenPrincipalCannotBeResolved() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        UserDetails missingUser = org.springframework.security.core.userdetails.User.withUsername("missing@test.com")
                .password("pw")
                .roles("MEMBER")
                .build();

        assertThatThrownBy(() -> controller.getProfile(null, missingUser))
                .hasMessageContaining("User not found");
    }
}
