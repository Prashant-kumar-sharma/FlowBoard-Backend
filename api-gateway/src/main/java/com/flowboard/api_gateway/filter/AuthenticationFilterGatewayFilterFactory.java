package com.flowboard.api_gateway.filter;

import com.flowboard.api_gateway.config.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuthenticationFilterGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationFilterGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    public AuthenticationFilterGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    public static class Config {
        // Configuration properties if needed
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip authentication for auth endpoints and public GET requests
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            if (path.contains("/api/v1/auth/login")
                    || path.contains("/api/v1/auth/register")
                    || path.contains("/api/v1/auth/refresh")
                    || path.contains("/oauth2")) {
                return chain.filter(exchange);
            }

            // Allow public GET requests to explore content
            if ("GET".equalsIgnoreCase(method)) {
                if (path.contains("/api/v1/workspaces/public") ||
                    path.contains("/api/v1/workspaces/") ||
                    path.contains("/api/v1/boards/") ||
                    path.contains("/api/v1/lists/board/") ||
                    path.contains("/api/v1/cards/list/") ||
                    path.contains("/api/v1/cards/board/") ||
                    path.contains("/api/v1/auth/users/")) {
                    return chain.filter(exchange);
                }
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }

            // Extract email/subject and userId, then inject into headers
            String email = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractUserRole(token);
            
            if (userId == null) {
                return onError(exchange, "JWT token is missing userId claim", HttpStatus.UNAUTHORIZED);
            }
            
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Role", role != null ? role : "MEMBER")
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        log.error("Authentication error: {}", err);
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }
}
