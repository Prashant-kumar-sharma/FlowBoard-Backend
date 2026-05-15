package com.flowboard.auth.security;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(jwtUtil, userRepository);
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:4200");
    }

    @Test
    void successHandlerRedirectsToFrontendWithJwtToken() throws Exception {
        OAuth2User principal = new DefaultOAuth2User(List.of(), Map.of("email", "alice@test.com"), "email");
        User user = User.builder().id(1L).email("alice@test.com").role(User.Role.MEMBER).build();
        when(authentication.getPrincipal()).thenReturn(principal);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(anyMap(), any())).thenReturn("jwt-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:4200/auth/oauth2/callback?token=jwt-token");
    }

    @Test
    void successHandlerRegistersMissingUserAndRedirects() throws Exception {
        OAuth2User principal = new DefaultOAuth2User(List.of(), Map.of("email", "missing@test.com"), "email");
        User newUser = User.builder().id(99L).email("missing@test.com").username("missing_9999").role(User.Role.MEMBER).build();
        when(authentication.getPrincipal()).thenReturn(principal);
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(jwtUtil.generateToken(anyMap(), any())).thenReturn("new-user-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:4200/auth/oauth2/callback?token=new-user-token");
    }
}
