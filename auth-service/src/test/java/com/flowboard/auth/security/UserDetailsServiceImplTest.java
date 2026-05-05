package com.flowboard.auth.security;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    void loadUserBuildsInheritedAuthoritiesForPlatformAdmin() {
        User user = User.builder()
                .email("admin@test.com")
                .passwordHash("pw")
                .role(User.Role.PLATFORM_ADMIN)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@test.com");

        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_PLATFORM_ADMIN", "ROLE_BOARD_ADMIN", "ROLE_MEMBER");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void loadUserMarksInactiveMemberAsDisabledAndLocked() {
        User user = User.builder()
                .email("member@test.com")
                .passwordHash("pw")
                .role(User.Role.MEMBER)
                .isActive(false)
                .build();
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("member@test.com");

        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_MEMBER");
        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserSupportsSystemRole() {
        User user = User.builder()
                .email("system@test.com")
                .passwordHash(null)
                .role(User.Role.SYSTEM)
                .isActive(true)
                .build();
        when(userRepository.findByEmail("system@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("system@test.com");

        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_SYSTEM");
        assertThat(details.getPassword()).isEmpty();
    }

    @Test
    void loadUserThrowsWhenRepositoryCannotFindUser() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
