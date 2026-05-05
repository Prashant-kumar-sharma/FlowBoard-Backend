package com.flowboard.auth.controller;

import com.flowboard.auth.dto.response.UserResponse;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.service.AuthService;
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
class AdminControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    private AdminController controller;
    private UserDetails adminPrincipal;

    @BeforeEach
    void setUp() {
        controller = new AdminController(authService, userRepository);
        adminPrincipal = org.springframework.security.core.userdetails.User.withUsername("admin@test.com")
                .password("pw")
                .roles("PLATFORM_ADMIN")
                .build();
        lenient().when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(User.builder().id(5L).email("admin@test.com").build()));
    }

    @Test
    void listAllUsersDelegatesToService() {
        List<UserResponse> users = List.of(UserResponse.builder().username("alice").build());
        when(authService.getAllUsers()).thenReturn(users);

        assertThat(controller.listAllUsers().getBody()).isEqualTo(users);
    }

    @Test
    void changeRoleDelegatesUsingResolvedActorId() {
        UserResponse response = UserResponse.builder().role(User.Role.BOARD_ADMIN).build();
        when(authService.changeRole(5L, 7L, "BOARD_ADMIN")).thenReturn(response);

        assertThat(controller.changeRole(adminPrincipal, 7L, Map.of("role", "BOARD_ADMIN")).getBody()).isEqualTo(response);
    }

    @Test
    void suspendRestoreAndDeleteDelegateToService() {
        assertThat(controller.suspend(adminPrincipal, 8L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.restore(8L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteUser(adminPrincipal, 8L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(authService).deactivateAccount(5L, 8L);
        verify(authService).reactivateAccount(8L);
        verify(authService).deleteUser(5L, 8L);
    }

    @Test
    void statsCountsActiveUsersAndAdmins() {
        List<UserResponse> users = List.of(
                UserResponse.builder().isActive(true).role(User.Role.PLATFORM_ADMIN).build(),
                UserResponse.builder().isActive(true).role(User.Role.MEMBER).build(),
                UserResponse.builder().isActive(false).role(User.Role.BOARD_ADMIN).build()
        );
        when(authService.getAllUsers()).thenReturn(users);

        Map<String, Object> body = controller.stats().getBody();

        assertThat(body).containsEntry("totalUsers", 3);
        assertThat(body).containsEntry("activeUsers", 2L);
        assertThat(body).containsEntry("platformAdmins", 1L);
    }

    @Test
    void securedEndpointsThrowWhenPrincipalCannotBeResolved() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        UserDetails missingAdmin = org.springframework.security.core.userdetails.User.withUsername("missing@test.com")
                .password("pw")
                .roles("PLATFORM_ADMIN")
                .build();

        assertThatThrownBy(() -> controller.changeRole(missingAdmin, 1L, Map.of("role", "MEMBER")))
                .hasMessageContaining("User not found");
    }
}
