package com.flowboard.auth.config;

import io.jsonwebtoken.io.Decoders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.MessageDigest;
import java.util.HexFormat;

@Slf4j
@Configuration
public class JwtDiagnosticsLogger {

    @Bean
    ApplicationRunner logJwtConfig(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {
        return args -> {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String fingerprint = HexFormat.of().formatHex(digest.digest(keyBytes)).substring(0, 12);
            log.info("JWT config loaded | keyBytes={} | fingerprint={} | expirationMs={}",
                    keyBytes.length, fingerprint, jwtExpirationMs);
        };
    }
}
