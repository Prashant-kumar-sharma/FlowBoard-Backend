package com.flowboard.auth.config;

import com.flowboard.auth.security.JwtAuthenticationFilter;
import com.flowboard.auth.security.OAuth2AuthenticationSuccessHandler;
import com.flowboard.auth.security.OAuth2UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {SecurityConfigTest.TestApplication.class, SecurityConfigTest.TestController.class},
        properties = "app.cors.allowed-origins=http://localhost:4200,http://localhost:4300"
)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @EnableAutoConfiguration
    @Import(SecurityConfig.class)
    static class TestApplication {
    }

    @RestController
    static class TestController {
        @PostMapping("/api/v1/auth/login")
        String login() {
            return "ok";
        }

        @GetMapping("/private")
        String privateEndpoint() {
            return "secret";
        }

        @GetMapping("/api/v1/admin/stats")
        String adminStats() {
            return "admin";
        }

        @GetMapping("/login/oauth2/probe")
        String oauthCallbackProbe() {
            return "oauth";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private AuthenticationConfiguration authenticationConfiguration;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private OAuth2UserServiceImpl oAuth2UserService;

    @MockBean
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        when(userDetailsService.loadUserByUsername(any())).thenReturn(
                User.withUsername("user@test.com").password(passwordEncoder.encode("pw")).roles("MEMBER").build()
        );
    }

    @Test
    void securityBeansAreCreated() throws Exception {
        assertThat(securityFilterChain).isNotNull();
        assertThat(authenticationProvider).isInstanceOf(DaoAuthenticationProvider.class);
        assertThat(authenticationConfiguration.getAuthenticationManager()).isNotNull();
        assertThat(passwordEncoder.matches("pw", passwordEncoder.encode("pw"))).isTrue();
    }

    @Test
    void corsConfigurationUsesAllowedOriginsAndMethods() {
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");

        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(configuration.getAllowedOriginPatterns())
                .containsExactly("http://localhost:4200", "http://localhost:4300");
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void publicAuthEndpointIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/private"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminEndpointAllowsPlatformAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats").with(user("admin").roles("PLATFORM_ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightRequestIsAccepted() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }

    @Test
    void oauthCallbackPathIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login/oauth2/probe"))
                .andExpect(status().isOk());
    }
}
