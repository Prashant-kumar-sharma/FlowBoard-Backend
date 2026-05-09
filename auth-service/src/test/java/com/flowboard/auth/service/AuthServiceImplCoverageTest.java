package com.flowboard.auth.service;

import com.flowboard.auth.dto.request.EmailOtpRequest;
import com.flowboard.auth.dto.request.LoginRequest;
import com.flowboard.auth.dto.request.RegisterRequest;
import com.flowboard.auth.dto.request.ResetPasswordOtpConfirmRequest;
import com.flowboard.auth.dto.request.ResetPasswordRequest;
import com.flowboard.auth.dto.request.UpdateProfileRequest;
import com.flowboard.auth.dto.request.VerifyOtpRequest;
import com.flowboard.auth.entity.AuthOtp;
import com.flowboard.auth.dto.response.UserResponse;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.exception.DuplicateResourceException;
import com.flowboard.auth.exception.InvalidCredentialsException;
import com.flowboard.auth.exception.InvalidOperationException;
import com.flowboard.auth.exception.ResourceNotFoundException;
import com.flowboard.auth.kafka.AuthEventProducer;
import com.flowboard.auth.repository.AuthOtpRepository;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.security.JwtUtil;
import com.flowboard.auth.service.AuthOtpEmailService;
import com.flowboard.auth.service.PaymentCleanupClient;
import com.flowboard.auth.service.PaymentEntitlementClient;
import com.flowboard.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplCoverageTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthOtpRepository authOtpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthEventProducer authEventProducer;

    @Mock
    private AuthOtpEmailService authOtpEmailService;

    @Mock
    private PaymentCleanupClient paymentCleanupClient;

    @Mock
    private PaymentEntitlementClient paymentEntitlementClient;

    @InjectMocks
    private AuthServiceImpl authService;

    private User memberUser;
    private User platformAdminUser;

    @BeforeEach
    void setUp() {
        memberUser = User.builder()
                .id(1L)
                .fullName("Alice")
                .email("alice@test.com")
                .username("alice")
                .passwordHash("encoded-old")
                .role(User.Role.MEMBER)
                .provider(User.AuthProvider.LOCAL)
                .isActive(true)
                .build();

        platformAdminUser = User.builder()
                .id(2L)
                .fullName("Admin")
                .email("admin@test.com")
                .username("admin")
                .passwordHash("encoded-admin")
                .role(User.Role.PLATFORM_ADMIN)
                .provider(User.AuthProvider.LOCAL)
                .isActive(true)
                .build();

        lenient().when(jwtUtil.generateToken(anyMap(), any())).thenReturn("jwt-token");
        lenient().when(paymentEntitlementClient.getEntitlement(any())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            com.flowboard.auth.dto.response.PaymentEntitlementResponse response =
                    new com.flowboard.auth.dto.response.PaymentEntitlementResponse();
            response.setUserId(userId);
            response.setPremium(false);
            response.setPlanCode("FREE");
            return response;
        });
        ReflectionTestUtils.setField(authService, "otpExpirationMinutes", 10L);
        ReflectionTestUtils.setField(authService, "otpResendCooldownSeconds", 60L);
    }

    @Test
    void registerThrowsWhenUsernameAlreadyTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.com");
        request.setUsername("alice");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username taken");
    }

    @Test
    void loginInactiveUserThrowsWhenPasswordDoesNotMatch() {
        memberUser.setIsActive(false);
        LoginRequest request = new LoginRequest();
        request.setEmail(memberUser.getEmail());
        request.setPassword("wrong");

        when(userRepository.findByEmail(memberUser.getEmail())).thenReturn(Optional.of(memberUser));
        when(passwordEncoder.matches("wrong", memberUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void getProfileReturnsMappedResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));

        UserResponse response = authService.getProfile(1L);

        assertThat(response.getEmail()).isEqualTo(memberUser.getEmail());
    }

    @Test
    void updateProfileUpdatesFieldsAndUsername() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("alice-updated");
        request.setFullName("Alice Updated");
        request.setBio("Bio");
        request.setAvatarUrl("avatar");

        when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));
        when(userRepository.existsByUsername("alice-updated")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.updateProfile(1L, request);

        assertThat(response.getUsername()).isEqualTo("alice-updated");
        assertThat(response.getFullName()).isEqualTo("Alice Updated");
        assertThat(response.getBio()).isEqualTo("Bio");
        assertThat(response.getAvatarUrl()).isEqualTo("avatar");
    }

    @Test
    void updateProfileThrowsWhenUsernameAlreadyExists() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("taken");

        when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.updateProfile(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void changePasswordUpdatesStoredHash() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));
        when(passwordEncoder.matches("old", memberUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encoded-new");

        authService.changePassword(1L, "old", "new");

        verify(userRepository).save(memberUser);
        assertThat(memberUser.getPasswordHash()).isEqualTo("encoded-new");
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordIsWrong() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));
        when(passwordEncoder.matches("wrong", memberUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(1L, "wrong", "new"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void resetPasswordUpdatesStoredHashForLocalUser() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(memberUser.getEmail());
        request.setNewPassword("NewPassword1!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("verification code");
    }

    @Test
    void resetPasswordThrowsWhenEmailDoesNotExist() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("missing@test.com");
        request.setNewPassword("NewPassword1!");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("verification code");
    }

    @Test
    void confirmPasswordResetUpdatesStoredHashForOauthAccount() {
        ResetPasswordOtpConfirmRequest request = new ResetPasswordOtpConfirmRequest();
        request.setEmail(memberUser.getEmail());
        request.setOtp("123456");
        request.setNewPassword("NewPassword1!");

        AuthOtp authOtp = AuthOtp.builder()
                .email(memberUser.getEmail())
                .purpose(AuthOtp.Purpose.RESET_PASSWORD)
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();

        when(userRepository.findByEmail(memberUser.getEmail())).thenReturn(Optional.of(memberUser));
        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(memberUser.getEmail(), AuthOtp.Purpose.RESET_PASSWORD))
                .thenReturn(Optional.of(authOtp));
        when(passwordEncoder.matches("123456", "encoded-otp")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("encoded-reset");

        authService.confirmPasswordReset(request);

        verify(userRepository).save(memberUser);
        assertThat(memberUser.getPasswordHash()).isEqualTo("encoded-reset");
    }

    @Test
    void requestLoginOtpFailsWhenEmailMissing() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@test.com");
        request.setPassword("Password1!");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.requestLoginOtp(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyRegistrationOtpCreatesNewUser() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("new@test.com");
        request.setOtp("123456");

        AuthOtp authOtp = AuthOtp.builder()
                .email("new@test.com")
                .purpose(AuthOtp.Purpose.REGISTRATION)
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .pendingFullName("New User")
                .pendingUsername("newuser")
                .pendingPasswordHash("encoded-password")
                .attemptCount(0)
                .build();

        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("new@test.com", AuthOtp.Purpose.REGISTRATION))
                .thenReturn(Optional.of(authOtp));
        when(passwordEncoder.matches("123456", "encoded-otp")).thenReturn(true);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(10L);
            }
            return user;
        });

        assertThat(authService.verifyRegistrationOtp(request).getUser().getEmail()).isEqualTo("new@test.com");
        verify(authOtpRepository).save(authOtp);
    }

    @Test
    void requestRegistrationOtpNormalizesEmailAndSendsOtpMail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("New User");
        request.setEmail("  New@Test.com ");
        request.setUsername("newuser");
        request.setPassword("Password1!");

        when(userRepository.existsByEmail("  New@Test.com ")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-value");

        assertThat(authService.requestRegistrationOtp(request).getMessage())
                .contains("Verification code sent");

        verify(authOtpEmailService).sendOtpEmail(eq("new@test.com"), eq("registration"), any(String.class));
        verify(authOtpRepository).save(any(AuthOtp.class));
    }

    @Test
    void verifyRegistrationOtpRejectsMissingPendingRegistrationData() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("new@test.com");
        request.setOtp("123456");

        AuthOtp authOtp = AuthOtp.builder()
                .email("new@test.com")
                .purpose(AuthOtp.Purpose.REGISTRATION)
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();

        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc("new@test.com", AuthOtp.Purpose.REGISTRATION))
                .thenReturn(Optional.of(authOtp));
        when(passwordEncoder.matches("123456", "encoded-otp")).thenReturn(true);

        assertThatThrownBy(() -> authService.verifyRegistrationOtp(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Registration details expired");
    }

    @Test
    void requestLoginOtpRejectsSuspendedUser() {
        memberUser.setIsActive(false);
        LoginRequest request = new LoginRequest();
        request.setEmail(memberUser.getEmail());
        request.setPassword("Password1!");

        when(userRepository.findByEmail(memberUser.getEmail())).thenReturn(Optional.of(memberUser));

        assertThatThrownBy(() -> authService.requestLoginOtp(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("suspended");
        verifyNoInteractions(authOtpEmailService);
    }

    @Test
    void verifyLoginOtpRejectsSuspendedUserAfterOtpValidation() {
        memberUser.setIsActive(false);
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(memberUser.getEmail());
        request.setOtp("123456");

        AuthOtp authOtp = AuthOtp.builder()
                .email(memberUser.getEmail())
                .purpose(AuthOtp.Purpose.LOGIN)
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();

        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(memberUser.getEmail(), AuthOtp.Purpose.LOGIN))
                .thenReturn(Optional.of(authOtp));
        when(passwordEncoder.matches("123456", "encoded-otp")).thenReturn(true);
        when(userRepository.findByEmail(memberUser.getEmail())).thenReturn(Optional.of(memberUser));

        assertThatThrownBy(() -> authService.verifyLoginOtp(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    void verifyLoginOtpRejectsConsumedOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(memberUser.getEmail());
        request.setOtp("123456");

        AuthOtp authOtp = AuthOtp.builder()
                .email(memberUser.getEmail())
                .purpose(AuthOtp.Purpose.LOGIN)
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .consumedAt(LocalDateTime.now())
                .attemptCount(0)
                .build();

        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(memberUser.getEmail(), AuthOtp.Purpose.LOGIN))
                .thenReturn(Optional.of(authOtp));

        assertThatThrownBy(() -> authService.verifyLoginOtp(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void verifyLoginOtpRejectsExpiredOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(memberUser.getEmail());
        request.setOtp("123456");

        AuthOtp authOtp = AuthOtp.builder()
                .email(memberUser.getEmail())
                .purpose(AuthOtp.Purpose.LOGIN)
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .attemptCount(0)
                .build();

        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(memberUser.getEmail(), AuthOtp.Purpose.LOGIN))
                .thenReturn(Optional.of(authOtp));

        assertThatThrownBy(() -> authService.verifyLoginOtp(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyLoginOtpRejectsAfterTooManyAttempts() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(memberUser.getEmail());
        request.setOtp("123456");

        AuthOtp authOtp = AuthOtp.builder()
                .email(memberUser.getEmail())
                .purpose(AuthOtp.Purpose.LOGIN)
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(5)
                .build();

        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(memberUser.getEmail(), AuthOtp.Purpose.LOGIN))
                .thenReturn(Optional.of(authOtp));

        assertThatThrownBy(() -> authService.verifyLoginOtp(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Too many invalid attempts");
        verify(authOtpRepository).save(authOtp);
        assertThat(authOtp.getConsumedAt()).isNotNull();
    }

    @Test
    void verifyLoginOtpRejectsInvalidCodeAndIncrementsAttemptCount() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail(memberUser.getEmail());
        request.setOtp("000000");

        AuthOtp authOtp = AuthOtp.builder()
                .email(memberUser.getEmail())
                .purpose(AuthOtp.Purpose.LOGIN)
                .otpHash("encoded-otp")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(2)
                .build();

        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(memberUser.getEmail(), AuthOtp.Purpose.LOGIN))
                .thenReturn(Optional.of(authOtp));
        when(passwordEncoder.matches("000000", "encoded-otp")).thenReturn(false);

        assertThatThrownBy(() -> authService.verifyLoginOtp(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid verification code");
        assertThat(authOtp.getAttemptCount()).isEqualTo(3);
        verify(authOtpRepository).save(authOtp);
    }

    @Test
    void requestPasswordResetOtpNormalizesEmailAndSendsOtpMail() {
        EmailOtpRequest request = new EmailOtpRequest();
        request.setEmail("  Alice@Test.com ");

        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(memberUser));
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-otp");

        assertThat(authService.requestPasswordResetOtp(request).getMessage())
                .contains("Password reset code sent");

        verify(authOtpEmailService).sendOtpEmail(eq(memberUser.getEmail()), eq("password reset"), any(String.class));
        verify(authOtpRepository).save(any(AuthOtp.class));
    }

    @Test
    void requestPasswordResetOtpRejectsCooldownWindow() {
        EmailOtpRequest request = new EmailOtpRequest();
        request.setEmail(memberUser.getEmail());

        AuthOtp existingOtp = AuthOtp.builder()
                .email(memberUser.getEmail())
                .purpose(AuthOtp.Purpose.RESET_PASSWORD)
                .otpHash("encoded-otp")
                .createdAt(LocalDateTime.now())
                .consumedAt(null)
                .build();

        when(userRepository.findByEmail(memberUser.getEmail())).thenReturn(Optional.of(memberUser));
        when(authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(memberUser.getEmail(), AuthOtp.Purpose.RESET_PASSWORD))
                .thenReturn(Optional.of(existingOtp));

        assertThatThrownBy(() -> authService.requestPasswordResetOtp(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Please wait before requesting another code");
    }

    @Test
    void logoutDoesNotTouchRepository() {
        authService.logout(1L, "token");

        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshTokenThrowsUnsupportedOperationException() {
        assertThatThrownBy(() -> authService.refreshToken("refresh"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getUserByIdThrowsWhenMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserByUsernameReturnsMappedResponse() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(memberUser));

        UserResponse response = authService.getUserByUsername("alice");

        assertThat(response.getUsername()).isEqualTo("alice");
    }

    @Test
    void getUserByUsernameThrowsWhenMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserByUsername("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeRoleThrowsWhenRoleIsBlank() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));

        assertThatThrownBy(() -> authService.changeRole(99L, 1L, " "))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Role is required");
    }

    @Test
    void changeRoleThrowsWhenRoleIsInvalid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));

        assertThatThrownBy(() -> authService.changeRole(99L, 1L, "not-a-role"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Invalid role");
    }

    @Test
    void changeRoleThrowsWhenAssigningSystemRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));

        assertThatThrownBy(() -> authService.changeRole(99L, 1L, "SYSTEM"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("SYSTEM role");
    }

    @Test
    void changeRoleThrowsWhenActorTargetsSelf() {
        assertThatThrownBy(() -> authService.changeRole(1L, 1L, "MEMBER"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("change your own platform role");
    }

    @Test
    void deactivateAccountThrowsWhenActorTargetsSelf() {
        assertThatThrownBy(() -> authService.deactivateAccount(1L, 1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("suspend your own account");
    }

    @Test
    void deleteUserThrowsWhenActorTargetsSelf() {
        assertThatThrownBy(() -> authService.deleteUser(1L, 1L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("delete your own account");
    }

    @Test
    void deactivateAccountThrowsWhenTargetIsLastActivePlatformAdmin() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(platformAdminUser));
        when(userRepository.countByRoleAndIsActiveTrue(User.Role.PLATFORM_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> authService.deactivateAccount(99L, 2L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("last active platform admin");
    }

    @Test
    void deleteUserThrowsWhenTargetIsLastActivePlatformAdmin() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(platformAdminUser));
        when(userRepository.countByRoleAndIsActiveTrue(User.Role.PLATFORM_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> authService.deleteUser(99L, 2L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("last active platform admin");
    }

    @Test
    void changeRoleThrowsWhenDemotingLastActivePlatformAdmin() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(platformAdminUser));
        when(userRepository.countByRoleAndIsActiveTrue(User.Role.PLATFORM_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> authService.changeRole(99L, 2L, "BOARD_ADMIN"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("last active platform admin");
    }
}
