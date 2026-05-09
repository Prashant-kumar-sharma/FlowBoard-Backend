package com.flowboard.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestPath = request.getRequestURI();
            if (requestPath.startsWith("/oauth2/") || requestPath.startsWith("/login/oauth2/")) {
                log.info("Processing OAuth request path in auth-service: {}", requestPath);
            }

            String jwt = parseJwt(request);
            if (jwt != null) {
                String username = jwtUtil.extractUsername(jwt);
                Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
                if (username != null && shouldAuthenticateWithJwt(currentAuthentication, username)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (userDetails.isEnabled() && userDetails.isAccountNonLocked() && jwtUtil.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldAuthenticateWithJwt(Authentication currentAuthentication, String username) {
        if (currentAuthentication == null) {
            return true;
        }

        // Prefer the bearer token for API requests even when an OAuth2 session already exists.
        // This keeps JWT-secured endpoints from inheriting OAuth-only authorities from the session.
        return !currentAuthentication.isAuthenticated() || !username.equals(currentAuthentication.getName());
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
