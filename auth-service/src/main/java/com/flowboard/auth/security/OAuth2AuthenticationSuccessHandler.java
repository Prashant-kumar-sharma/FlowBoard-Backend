package com.flowboard.auth.security;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email == null || email.isBlank()) {
            log.error("Google OAuth success but email is missing from attributes!");
            response.sendRedirect(frontendUrl + "/auth/login?oauthError=Google+did+not+provide+an+email+address");
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Registering new user via OAuth2: {}", email);
            String name = oAuth2User.getAttribute("name");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }
            User newUser = User.builder()
                    .email(email)
                    .fullName(name)
                    .username(email.split("@")[0] + "_" + (System.currentTimeMillis() % 10000))
                    .provider(User.AuthProvider.GOOGLE)
                    .role(User.Role.MEMBER)
                    .isActive(true)
                    .build();
            return userRepository.save(newUser);
        });

        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();

        String token = jwtUtil.generateToken(java.util.Map.of("userId", user.getId(), "role", user.getRole().name()), userDetails);
        String redirectUrl = frontendUrl + "/auth/oauth2/callback?token=" + token;

        log.info("OAuth2 login successful for: {}, redirecting to Angular", email);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
