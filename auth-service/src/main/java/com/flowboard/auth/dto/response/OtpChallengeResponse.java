package com.flowboard.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpChallengeResponse {
    private String message;
    private long expiresInSeconds;
}
