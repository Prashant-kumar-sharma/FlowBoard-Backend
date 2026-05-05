package com.flowboard.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User userDetails;
    private String secret;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        secret = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 60_000L);
        userDetails = (User) User.withUsername("alice@test.com").password("pw").roles("MEMBER").build();
    }

    @Test
    void generateAndValidateTokenForSameUser() {
        String token = jwtUtil.generateToken(Map.of("userId", 1L), userDetails);

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice@test.com");
        assertThat(jwtUtil.extractExpiration(token)).isAfter(new Date());
        assertThat(jwtUtil.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void generateTokenWithoutExtraClaimsUsesDefaultOverload() {
        String token = jwtUtil.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void validateTokenReturnsFalseForDifferentUser() {
        String token = jwtUtil.generateToken(userDetails);
        User otherUser = (User) User.withUsername("other@test.com").password("pw").roles("MEMBER").build();

        assertThat(jwtUtil.validateToken(token, otherUser)).isFalse();
    }

    @Test
    void validateTokenReturnsFalseForMalformedToken() {
        assertThat(jwtUtil.validateToken("not-a-token", userDetails)).isFalse();
    }

    @Test
    void validateTokenReturnsFalseForExpiredToken() {
        String expiredToken = Jwts.builder()
                .subject("alice@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000L))
                .expiration(new Date(System.currentTimeMillis() - 5_000L))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .compact();

        assertThat(jwtUtil.validateToken(expiredToken, userDetails)).isFalse();
    }
}
