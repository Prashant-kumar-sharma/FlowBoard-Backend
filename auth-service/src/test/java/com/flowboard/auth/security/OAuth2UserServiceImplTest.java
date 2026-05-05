package com.flowboard.auth.security;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private OAuth2UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OAuth2UserServiceImpl(userRepository);
    }

    @Test
    void processOAuth2UserCreatesNewGoogleUser() {
        OAuth2User user = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "email", "alice@test.com",
                        "name", "Alice",
                        "sub", "google-1",
                        "picture", "avatar"
                ),
                "email"
        );
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("alice")).thenReturn(false);

        ReflectionTestUtils.invokeMethod(service, "processOAuth2User", "google", user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo(User.AuthProvider.GOOGLE);
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
    }

    @Test
    void processOAuth2UserUpdatesExistingLocalGithubUser() {
        User existing = User.builder()
                .email("bob@test.com")
                .provider(User.AuthProvider.LOCAL)
                .avatarUrl("")
                .isActive(true)
                .build();
        OAuth2User user = new DefaultOAuth2User(
                List.of(),
                Map.of(
                        "email", "bob@test.com",
                        "name", "Bob",
                        "login", "boblogin",
                        "id", 99,
                        "avatar_url", "avatar"
                ),
                "email"
        );
        when(userRepository.findByEmail("bob@test.com")).thenReturn(Optional.of(existing));

        ReflectionTestUtils.invokeMethod(service, "processOAuth2User", "github", user);

        assertThat(existing.getProvider()).isEqualTo(User.AuthProvider.GITHUB);
        assertThat(existing.getProviderId()).isEqualTo("99");
        assertThat(existing.getAvatarUrl()).isEqualTo("avatar");
        verify(userRepository).save(existing);
    }

    @Test
    void processOAuth2UserSkipsSaveWhenEmailIsMissing() {
        OAuth2User user = new DefaultOAuth2User(List.of(), Map.of("name", "No Email"), "name");

        ReflectionTestUtils.invokeMethod(service, "processOAuth2User", "google", user);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void processOAuth2UserRejectsUnsupportedProvider() {
        OAuth2User user = new DefaultOAuth2User(List.of(), Map.of("email", "x@test.com"), "email");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(service, "processOAuth2User", "discord", user))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void generateUsernameAppendsSuffixUntilFree() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        when(userRepository.existsByUsername("alice1")).thenReturn(true);
        when(userRepository.existsByUsername("alice2")).thenReturn(false);

        String username = ReflectionTestUtils.invokeMethod(service, "generateUsername", "alice@test.com");

        assertThat(username).isEqualTo("alice2");
    }
}
