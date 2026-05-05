package com.flowboard.auth.config;

import com.flowboard.auth.security.JwtAuthenticationFilter;
import com.flowboard.auth.security.OAuth2UserServiceImpl;
import com.flowboard.auth.security.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ROLE_PLATFORM_ADMIN = "PLATFORM_ADMIN";

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final OAuth2UserServiceImpl oAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .authorizeHttpRequests(auth -> auth

                // GUEST
                .requestMatchers(HttpMethod.GET, "/api/v1/boards/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/workspaces/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/users/**").permitAll()
                .requestMatchers("/api/v1/auth/internal/**").permitAll()

                // AUTH
                .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/register/request-otp",
                        "/api/v1/auth/register/verify-otp",
                        "/api/v1/auth/login",
                        "/api/v1/auth/login/request-otp",
                        "/api/v1/auth/login/verify-otp",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/reset-password/request-otp",
                        "/api/v1/auth/reset-password/confirm",
                        "/oauth2/**",
                        "/api/v1/auth/oauth2/**"
                ).permitAll()

                // SWAGGER / HEALTH
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api-docs/**",
                        "/actuator/health"
                ).permitAll()

                // PLATFORM_ADMIN
                .requestMatchers("/api/v1/admin/**").hasRole(ROLE_PLATFORM_ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole(ROLE_PLATFORM_ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/auth/users/*/deactivate").hasRole(ROLE_PLATFORM_ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/auth/users/*/reactivate").hasRole(ROLE_PLATFORM_ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/v1/audit-logs/**").hasRole(ROLE_PLATFORM_ADMIN)
                .requestMatchers("/api/v1/notifications/broadcast").hasRole(ROLE_PLATFORM_ADMIN)

                // BOARD_ADMIN or PLATFORM_ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/v1/workspaces/**")
                .hasAnyRole("BOARD_ADMIN", ROLE_PLATFORM_ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/boards/**")
                .hasAnyRole("BOARD_ADMIN", ROLE_PLATFORM_ADMIN)

                // SYSTEM
                .requestMatchers("/api/v1/system/**").hasRole("SYSTEM")

                // ALL OTHERS
                .anyRequest().authenticated()
            )

            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
