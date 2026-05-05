package com.flowboard.api_gateway.filter;

import com.flowboard.api_gateway.config.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class AuthenticationFilterGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final Set<String> AUTH_WHITELIST = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh",
            "/oauth2"
    );

    private static final Set<String> PUBLIC_GET_WHITELIST = Set.of(
            "/api/v1/workspaces/public",
            "/api/v1/workspaces/",
            "/api/v1/boards/",
            "/api/v1/lists/board/",
            "/api/v1/cards/list/",
            "/api/v1/cards/board/",
            "/api/v1/auth/users/"
    );

    private static final ParameterizedTypeReference<Map<String, Object>> USER_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final JwtUtil jwtUtil;
    private final WebClient webClient;

    @Value("${auth.service.internal-base-url:http://localhost:8081/api/v1/auth/internal}")
    private String authServiceInternalBaseUrl;

    public AuthenticationFilterGatewayFilterFactory(JwtUtil jwtUtil, WebClient.Builder webClientBuilder) {
        super(Object.class);
        this.jwtUtil = jwtUtil;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (shouldSkipAuthentication(request, path)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (!hasBearerToken(authHeader)) {
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
            
            return loadUser(userId).flatMap(user -> {
                if (!isUserActive(user)) {
                    return onError(exchange, "Account is suspended", HttpStatus.FORBIDDEN, "SUSPENDED");
                }

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Email", email)
                        .header("X-User-Id", String.valueOf(userId))
                        .header("X-User-Role", role != null ? role : "MEMBER")
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            }).onErrorResume(ex -> {
                log.error("Failed to validate account status for userId={}: {}", userId, ex.getMessage());
                return onError(exchange, "Could not verify account status", HttpStatus.UNAUTHORIZED);
            });
        };
    }

    private boolean shouldSkipAuthentication(ServerHttpRequest request, String path) {
        return matchesAny(path, AUTH_WHITELIST) || isPublicGetRequest(request, path);
    }

    private boolean isPublicGetRequest(ServerHttpRequest request, String path) {
        return HttpMethod.GET.equals(request.getMethod()) && matchesAny(path, PUBLIC_GET_WHITELIST);
    }

    private boolean matchesAny(String path, Set<String> allowedPaths) {
        return allowedPaths.stream().anyMatch(path::contains);
    }

    private boolean hasBearerToken(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private boolean isUserActive(Map<String, Object> user) {
        Object activeValue = user.get("isActive");
        return activeValue instanceof Boolean active
                ? active
                : Boolean.parseBoolean(String.valueOf(activeValue));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        return onError(exchange, err, httpStatus, null);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus, String accountStatus) {
        log.error("Authentication error: {}", err);
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (accountStatus != null) {
            exchange.getResponse().getHeaders().add("X-Account-Status", accountStatus);
        }
        byte[] body = ("{\"message\":\"" + err + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private Mono<Map<String, Object>> loadUser(Long userId) {
        return webClient.get()
                .uri(authServiceInternalBaseUrl + "/users/" + userId)
                .retrieve()
                .bodyToMono(USER_RESPONSE_TYPE);
    }
}
