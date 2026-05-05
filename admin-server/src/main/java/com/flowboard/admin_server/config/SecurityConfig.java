package com.flowboard.admin_server.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminServerProperties adminServer;
    private final boolean csrfCookieSecure;

    public SecurityConfig(
            AdminServerProperties adminServer,
            @Value("${app.security.csrf.cookie-secure:false}") boolean csrfCookieSecure) {
        this.adminServer = adminServer;
        this.csrfCookieSecure = csrfCookieSecure;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServer.getContextPath() + "/");
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .path(this.adminServer.getContextPath() + "/")
                .sameSite("Strict")
                .secure(csrfCookieSecure));

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(this.adminServer.getContextPath() + "/assets/**").permitAll()
                .requestMatchers(this.adminServer.getContextPath() + "/login").permitAll()
                .requestMatchers(this.adminServer.getContextPath() + "/actuator/**").permitAll()
                .requestMatchers(this.adminServer.getContextPath() + "/instances").permitAll()
                .anyRequest().authenticated()
        )
        .formLogin(formLogin -> formLogin
                .loginPage(this.adminServer.getContextPath() + "/login")
                .successHandler(successHandler)
        )
        .logout(logout -> logout.logoutUrl(this.adminServer.getContextPath() + "/logout"))
        .httpBasic(Customizer.withDefaults())
        .csrf(csrf -> csrf
                // Spring Boot Admin's browser UI needs to read the CSRF token cookie and echo it back.
                // Keep CSRF enabled for the UI and only bypass it for machine-to-machine registration endpoints.
                .csrfTokenRepository(csrfTokenRepository)
                .ignoringRequestMatchers(
                        new AntPathRequestMatcher(this.adminServer.getContextPath() + "/instances", "POST"),
                        new AntPathRequestMatcher(this.adminServer.getContextPath() + "/instances/*", "DELETE"),
                        new AntPathRequestMatcher(this.adminServer.getContextPath() + "/actuator/**")
                )
        );

        return http.build();
    }
}
