package com.flowboard.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;

class JwtDiagnosticsLoggerTest {

    @Test
    void runnerLogsDerivedJwtFingerprintWithoutThrowing() throws Exception {
        JwtDiagnosticsLogger logger = new JwtDiagnosticsLogger();
        String secret = Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));

        ApplicationRunner runner = logger.logJwtConfig(secret, 86_400_000L);

        assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();
    }
}
