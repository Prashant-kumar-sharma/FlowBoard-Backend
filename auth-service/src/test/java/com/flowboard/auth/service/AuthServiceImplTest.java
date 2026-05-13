package com.flowboard.auth.service;

import com.flowboard.auth.dto.request.LoginRequest;
import com.flowboard.auth.dto.request.RegisterRequest;
import com.flowboard.auth.dto.response.AuthResponse;
import com.flowboard.auth.dto.response.PaymentEntitlementResponse;
import com.flowboard.auth.dto.response.UserResponse;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.exception.DuplicateResourceException;
import com.flowboard.auth.exception.InvalidCredentialsException;
import com.flowboard.auth.exception.ResourceNotFoundException;
import com.flowboard.auth.kafka.AuthEventProducer;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.security.JwtUtil;
import com.flowboard.auth.service.PaymentCleanupClient;
import com.flowboard.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl — all actor scenarios")
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @Mock AuthEventProducer authEventProducer;
    @Mock PaymentCleanupClient paymentCleanupClient;
    @Mock PaymentEntitlementClient paymentEntitlementClient;
    @InjectMocks AuthServiceImpl authService;

    private User memberUser;
    private User boardAdminUser;
    private User platformAdminUser;

    @BeforeEach
    void setUp() {
        memberUser = User.builder().id(1L).fullName("Alice").email("alice@test.com")
                .username("alice").passwordHash("$2a$hashed").role(User.Role.MEMBER)
                .provider(User.AuthProvider.LOCAL).isActive(true).build();

        boardAdminUser = User.builder().id(2L).fullName("Bob").email("bob@test.com")
                .username("bob").passwordHash("$2a$hashed").role(User.Role.BOARD_ADMIN)
                .provider(User.AuthProvider.LOCAL).isActive(true).build();

        platformAdminUser = User.builder().id(3L).fullName("Carol").email("carol@test.com")
                .username("carol").passwordHash("$2a$hashed").role(User.Role.PLATFORM_ADMIN)
                .provider(User.AuthProvider.LOCAL).isActive(true).build();

        lenient().when(jwtUtil.generateToken(anyMap(), any())).thenReturn("jwt.token");
        lenient().when(paymentEntitlementClient.getEntitlement(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            PaymentEntitlementResponse response = new PaymentEntitlementResponse();
            response.setUserId(userId);
            response.setPremium(false);
            response.setPlanCode("FREE");
            return response;
        });
    }

    // ── GUEST: Registration ───────────────────────────────────────────────

    @Nested @DisplayName("GUEST — register")
    class GuestRegister {

        @Test
        void should_registerNewUser_as_MEMBER_role() {
            RegisterRequest req = new RegisterRequest();
            req.setFullName("Dave"); req.setEmail("dave@test.com");
            req.setUsername("dave"); req.setPassword("password123");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(userRepository.save(any())).thenReturn(memberUser);
            when(jwtUtil.generateToken(anyMap(), any())).thenReturn("jwt.token");

            AuthResponse resp = authService.register(req);

            assertThat(resp.getAccessToken()).isEqualTo("jwt.token");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            // New registrations are always MEMBER — never ADMIN
            assertThat(captor.getValue().getRole()).isEqualTo(User.Role.MEMBER);
        }

        @Test
        void should_throwDuplicate_when_emailTaken() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("alice@test.com");
            when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(DuplicateResourceException.class);
            verify(userRepository, never()).save(any());
        }
    }

    // ── MEMBER: login ─────────────────────────────────────────────────────

    @Nested @DisplayName("MEMBER — login")
    class MemberLogin {

        @Test
        void should_loginSuccessfully_when_credentialsValid() {
            LoginRequest req = new LoginRequest();
            req.setEmail("alice@test.com"); req.setPassword("password");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(mock(org.springframework.security.core.Authentication.class));
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(memberUser));
            when(jwtUtil.generateToken(anyMap(), any())).thenReturn("valid.token");

            AuthResponse resp = authService.login(req);
            assertThat(resp.getAccessToken()).isEqualTo("valid.token");
            assertThat(resp.getUser().getRole()).isEqualTo(User.Role.MEMBER);
        }

        @Test
        void should_throwInvalidCredentials_when_passwordWrong() {
            LoginRequest req = new LoginRequest();
            req.setEmail("alice@test.com"); req.setPassword("wrong");
            when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(memberUser));
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        void should_loginToSuspendedSession_when_accountSuspended_and_passwordMatches() {
            memberUser.setIsActive(false);
            LoginRequest req = new LoginRequest();
            req.setEmail("alice@test.com"); req.setPassword("pw");
            when(passwordEncoder.matches("pw", memberUser.getPasswordHash())).thenReturn(true);
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(memberUser));
            when(jwtUtil.generateToken(anyMap(), any())).thenReturn("suspended.token");

            AuthResponse resp = authService.login(req);
            assertThat(resp.getAccessToken()).isEqualTo("suspended.token");
            assertThat(resp.getUser().getIsActive()).isFalse();
            verify(authenticationManager, never()).authenticate(any());
        }
    }

    // ── PLATFORM_ADMIN: user management ──────────────────────────────────

    @Nested @DisplayName("PLATFORM_ADMIN — user management")
    class PlatformAdminUserManagement {

        @Test
        void should_deactivateUser_when_platformAdminRequests() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));
            authService.deactivateAccount(1L);
            verify(userRepository).save(argThat(u -> !u.getIsActive()));
            verify(authEventProducer).sendAccountSuspended(memberUser);
        }

        @Test
        void should_reactivateUser_when_platformAdminRequests() {
            memberUser.setIsActive(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));
            authService.reactivateAccount(1L);
            verify(userRepository).save(argThat(User::getIsActive));
            verify(authEventProducer).sendAccountRestored(memberUser);
        }

        @Test
        void should_deleteUser_permanently() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));
            authService.deleteUser(1L);
            verify(paymentCleanupClient).deleteUserPaymentData(1L);
            verify(userRepository).delete(memberUser);
        }

        @Test
        void should_promoteUser_to_BOARD_ADMIN() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(memberUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            UserResponse resp = authService.changeRole(1L, "BOARD_ADMIN");
            assertThat(resp.getRole()).isEqualTo(User.Role.BOARD_ADMIN);
        }

        @Test
        void should_promoteToPlatformAdmin() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(boardAdminUser));
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            UserResponse resp = authService.changeRole(2L, "PLATFORM_ADMIN");
            assertThat(resp.getRole()).isEqualTo(User.Role.PLATFORM_ADMIN);
        }

        @Test
        void should_throwNotFound_when_userMissingOnDelete() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> authService.deleteUser(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void should_returnAllUsers_for_platformAdmin() {
            when(userRepository.findAll()).thenReturn(List.of(memberUser, boardAdminUser, platformAdminUser));
            List<UserResponse> all = authService.getAllUsers();
            assertThat(all).hasSize(3);
        }
    }

    // ── MEMBER: search (used to invite to workspace/board) ────────────────

    @Test
    void should_returnMatchingUsers_for_memberSearch() {
        when(userRepository.searchByFullNameOrUsername("alice")).thenReturn(List.of(memberUser));
        List<UserResponse> results = authService.searchUsers("alice");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUsername()).isEqualTo("alice");
    }
}
