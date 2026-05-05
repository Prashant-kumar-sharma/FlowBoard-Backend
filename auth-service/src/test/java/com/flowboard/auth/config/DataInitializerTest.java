package com.flowboard.auth.config;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new DataInitializer(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@test.com");
        ReflectionTestUtils.setField(initializer, "adminPassword", "secret123");
        ReflectionTestUtils.setField(initializer, "adminFullName", "Platform Administrator");
    }

    @Test
    void runCreatesDefaultAdminWhenMissing() throws Exception {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");

        initializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(User.Role.PLATFORM_ADMIN);
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@test.com");
        assertThat(captor.getValue().getFullName()).isEqualTo("Platform Administrator");
    }

    @Test
    void runRestoresExistingAdminWhenRoleOrStatusChanged() throws Exception {
        User admin = User.builder()
                .email("admin@test.com")
                .role(User.Role.MEMBER)
                .isActive(false)
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");

        initializer.run();

        verify(userRepository).save(admin);
        assertThat(admin.getRole()).isEqualTo(User.Role.PLATFORM_ADMIN);
        assertThat(admin.getIsActive()).isTrue();
        assertThat(admin.getProvider()).isEqualTo(User.AuthProvider.LOCAL);
        assertThat(admin.getPasswordHash()).isEqualTo("encoded");
    }

    @Test
    void runLeavesExistingHealthyAdminUntouched() throws Exception {
        User admin = User.builder()
                .email("admin@test.com")
                .role(User.Role.PLATFORM_ADMIN)
                .isActive(true)
                .provider(User.AuthProvider.LOCAL)
                .passwordHash("encoded")
                .build();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret123", "encoded")).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(admin);
    }
}
