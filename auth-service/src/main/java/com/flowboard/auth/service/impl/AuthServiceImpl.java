package com.flowboard.auth.service.impl;

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
import com.flowboard.auth.entity.AuthOtp;
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
import com.flowboard.auth.service.AuthService;
import com.flowboard.auth.service.PaymentCleanupClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    private static final String USER_NOT_FOUND_PREFIX = "User not found: ";
    private static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";
    private static final String ROLE_BOARD_ADMIN = "ROLE_BOARD_ADMIN";
    private static final String ROLE_MEMBER = "ROLE_MEMBER";
    private static final String ROLE_SYSTEM = "ROLE_SYSTEM";
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AuthOtpRepository authOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthEventProducer authEventProducer;
    private final AuthOtpEmailService authOtpEmailService;
    private final PaymentCleanupClient paymentCleanupClient;

    @Value("${app.otp.expiration-minutes:10}")
    private long otpExpirationMinutes;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long otpResendCooldownSeconds;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username taken: " + request.getUsername());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.MEMBER)
                .provider(User.AuthProvider.LOCAL)
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        log.info("New MEMBER registered: {}", saved.getEmail());
        return buildAuthResponse(saved);
    }

    @Override
    public OtpChallengeResponse requestRegistrationOtp(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username taken: " + request.getUsername());
        }

        String otp = createOtp(
                request.getEmail(),
                AuthOtp.Purpose.REGISTRATION,
                request.getFullName(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword())
        );
        authOtpEmailService.sendOtpEmail(normalizeEmail(request.getEmail()), "registration", otp);
        return otpChallenge("Verification code sent to your email.");
    }

    @Override
    public AuthResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        AuthOtp otp = validateOtp(normalizedEmail, request.getOtp(), AuthOtp.Purpose.REGISTRATION);

        if (otp.getPendingFullName() == null || otp.getPendingUsername() == null || otp.getPendingPasswordHash() == null) {
            throw new InvalidOperationException("Registration details expired. Please request a new code.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email already registered: " + normalizedEmail);
        }
        if (userRepository.existsByUsername(otp.getPendingUsername())) {
            throw new DuplicateResourceException("Username taken: " + otp.getPendingUsername());
        }

        User user = User.builder()
                .fullName(otp.getPendingFullName())
                .email(normalizedEmail)
                .username(otp.getPendingUsername())
                .passwordHash(otp.getPendingPasswordHash())
                .role(User.Role.MEMBER)
                .provider(User.AuthProvider.LOCAL)
                .isActive(true)
                .build();

        consumeOtp(otp);
        User saved = userRepository.save(user);
        log.info("New MEMBER registered via OTP: {}", saved.getEmail());
        return buildAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new InvalidCredentialsException("Invalid email or password");
            }
            return buildAuthResponse(user);
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return buildAuthResponse(user);
    }

    @Override
    public OtpChallengeResponse requestLoginOtp(EmailOtpRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for: " + request.getEmail()));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidOperationException("Your account is suspended. Please contact an administrator.");
        }

        String otp = createOtp(user.getEmail(), AuthOtp.Purpose.LOGIN, null, null, null);
        authOtpEmailService.sendOtpEmail(user.getEmail(), "sign in", otp);
        return otpChallenge("Sign-in code sent to your email.");
    }

    @Override
    public AuthResponse verifyLoginOtp(VerifyOtpRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        AuthOtp otp = validateOtp(normalizedEmail, request.getOtp(), AuthOtp.Purpose.LOGIN);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for: " + request.getEmail()));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidOperationException("Your account is suspended. Please contact an administrator.");
        }

        consumeOtp(otp);
        return buildAuthResponse(user);
    }

    @Override
    public OtpChallengeResponse requestPasswordResetOtp(EmailOtpRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("No account found for: " + request.getEmail()));
        String otp = createOtp(user.getEmail(), AuthOtp.Purpose.RESET_PASSWORD, null, null, null);
        authOtpEmailService.sendOtpEmail(user.getEmail(), "password reset", otp);
        return otpChallenge("Password reset code sent to your email.");
    }

    @Override
    public void confirmPasswordReset(ResetPasswordOtpConfirmRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        AuthOtp otp = validateOtp(normalizedEmail, request.getOtp(), AuthOtp.Purpose.RESET_PASSWORD);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for: " + request.getEmail()));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        consumeOtp(otp);
        log.info("Password reset completed for {}", user.getEmail());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        throw new InvalidOperationException("Password reset now requires an email verification code.");
    }

    @Override
    public void logout(Long userId, String accessToken) {
        log.info("User {} logged out", userId);
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenValue) {
        throw new UnsupportedOperationException("Refresh tokens are disabled in this version");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findById(userId));
    }

    @Override
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException("Username taken: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void deactivateAccount(Long userId) {
        deactivateAccount(null, userId);
    }

    @Override
    public void deactivateAccount(Long actorUserId, Long userId) {
        ensureNotSelfTarget(actorUserId, userId, "suspend your own account");
        User user = findById(userId);
        ensureNotLastActivePlatformAdmin(user, "suspend the last active platform admin");
        user.setIsActive(false);
        userRepository.save(user);
        authEventProducer.sendAccountSuspended(user);
        log.info("Account suspended: {}", user.getEmail());
    }

    @Override
    public void reactivateAccount(Long userId) {
        User user = findById(userId);
        user.setIsActive(true);
        userRepository.save(user);
        authEventProducer.sendAccountRestored(user);
    }

    @Override
    public void deleteUser(Long userId) {
        deleteUser(null, userId);
    }

    @Override
    public void deleteUser(Long actorUserId, Long userId) {
        ensureNotSelfTarget(actorUserId, userId, "delete your own account");
        User user = findById(userId);
        ensureNotLastActivePlatformAdmin(user, "delete the last active platform admin");
        paymentCleanupClient.deleteUserPaymentData(userId);
        userRepository.delete(user);
        log.info("PLATFORM_ADMIN permanently deleted user: {}", user.getEmail());
    }

    @Override
    public UserResponse changeRole(Long userId, String newRole) {
        return changeRole(null, userId, newRole);
    }

    @Override
    public UserResponse changeRole(Long actorUserId, Long userId, String newRole) {
        ensureNotSelfTarget(actorUserId, userId, "change your own platform role");
        User user = findById(userId);
        User.Role requestedRole = parseRole(newRole);
        if (user.getRole() == User.Role.PLATFORM_ADMIN && requestedRole != User.Role.PLATFORM_ADMIN) {
            ensureNotLastActivePlatformAdmin(user, "demote the last active platform admin");
        }
        user.setRole(requestedRole);
        return UserResponse.from(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String query) {
        return userRepository.searchByFullNameOrUsername(query)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return UserResponse.from(findById(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_PREFIX + username));
        return UserResponse.from(user);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_PREFIX + id));
    }

    private User.Role parseRole(String newRole) {
        if (newRole == null || newRole.isBlank()) {
            throw new InvalidOperationException("Role is required");
        }
        try {
            User.Role role = User.Role.valueOf(newRole.toUpperCase(Locale.ROOT));
            if (role == User.Role.SYSTEM) {
                throw new InvalidOperationException("SYSTEM role cannot be assigned from the admin console");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationException("Invalid role: " + newRole);
        }
    }

    private void ensureNotSelfTarget(Long actorUserId, Long targetUserId, String action) {
        if (actorUserId != null && actorUserId.equals(targetUserId)) {
            throw new InvalidOperationException("You cannot " + action);
        }
    }

    private void ensureNotLastActivePlatformAdmin(User user, String action) {
        if (user.getRole() == User.Role.PLATFORM_ADMIN
                && Boolean.TRUE.equals(user.getIsActive())
                && userRepository.countByRoleAndIsActiveTrue(User.Role.PLATFORM_ADMIN) <= 1) {
            throw new InvalidOperationException("You cannot " + action);
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(tokenFor(user))
                .user(UserResponse.from(user))
                .build();
    }

    private OtpChallengeResponse otpChallenge(String message) {
        return OtpChallengeResponse.builder()
                .message(message)
                .expiresInSeconds(otpExpirationMinutes * 60)
                .build();
    }

    private String createOtp(String email, AuthOtp.Purpose purpose, String pendingFullName, String pendingUsername, String pendingPasswordHash) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();

        authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(normalizedEmail, purpose)
                .ifPresent(existing -> {
                    if (existing.getConsumedAt() == null
                            && existing.getCreatedAt() != null
                            && existing.getCreatedAt().plusSeconds(otpResendCooldownSeconds).isAfter(now)) {
                        throw new InvalidOperationException("Please wait before requesting another code.");
                    }
                    if (existing.getConsumedAt() == null) {
                        existing.setConsumedAt(now);
                        authOtpRepository.save(existing);
                    }
                });

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        AuthOtp authOtp = AuthOtp.builder()
                .email(normalizedEmail)
                .purpose(purpose)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(now.plusMinutes(otpExpirationMinutes))
                .pendingFullName(pendingFullName)
                .pendingUsername(pendingUsername)
                .pendingPasswordHash(pendingPasswordHash)
                .build();
        authOtpRepository.save(authOtp);
        return otp;
    }

    private AuthOtp validateOtp(String email, String otp, AuthOtp.Purpose purpose) {
        AuthOtp authOtp = authOtpRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new InvalidOperationException("No active verification code found. Please request a new code."));

        if (authOtp.getConsumedAt() != null) {
            throw new InvalidOperationException("This verification code has already been used.");
        }
        if (authOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOperationException("This verification code has expired. Please request a new code.");
        }
        if (authOtp.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
            authOtp.setConsumedAt(LocalDateTime.now());
            authOtpRepository.save(authOtp);
            throw new InvalidOperationException("Too many invalid attempts. Please request a new code.");
        }
        if (!passwordEncoder.matches(otp, authOtp.getOtpHash())) {
            authOtp.setAttemptCount(authOtp.getAttemptCount() + 1);
            authOtpRepository.save(authOtp);
            throw new InvalidCredentialsException("Invalid verification code.");
        }

        return authOtp;
    }

    private void consumeOtp(AuthOtp authOtp) {
        authOtp.setConsumedAt(LocalDateTime.now());
        authOtpRepository.save(authOtp);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String tokenFor(User user) {
        List<SimpleGrantedAuthority> auths = buildAuthorities(user.getRole());
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
                .authorities(auths)
                .build();
        return jwtUtil.generateToken(java.util.Map.of("userId", user.getId(), "role", user.getRole().name()), userDetails);
    }

    private List<SimpleGrantedAuthority> buildAuthorities(User.Role role) {
        return switch (role) {
            case PLATFORM_ADMIN -> List.of(
                    new SimpleGrantedAuthority(ROLE_PLATFORM_ADMIN),
                    new SimpleGrantedAuthority(ROLE_BOARD_ADMIN),
                    new SimpleGrantedAuthority(ROLE_MEMBER));
            case BOARD_ADMIN -> List.of(
                    new SimpleGrantedAuthority(ROLE_BOARD_ADMIN),
                    new SimpleGrantedAuthority(ROLE_MEMBER));
            case SYSTEM -> List.of(new SimpleGrantedAuthority(ROLE_SYSTEM));
            default -> List.of(new SimpleGrantedAuthority(ROLE_MEMBER));
        };
    }
}
