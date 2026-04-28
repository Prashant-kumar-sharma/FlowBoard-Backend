package com.flowboard.admin_server.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
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

    public SecurityConfig(AdminServerProperties adminServer) {
        this.adminServer = adminServer;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServer.getContextPath() + "/");

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
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                        new AntPathRequestMatcher(this.adminServer.getContextPath() + "/instances", "POST"),
                        new AntPathRequestMatcher(this.adminServer.getContextPath() + "/instances/*", "DELETE"),
                        new AntPathRequestMatcher(this.adminServer.getContextPath() + "/actuator/**")
                )
        );

        return http.build();
    }
}
