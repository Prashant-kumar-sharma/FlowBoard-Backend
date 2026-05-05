package com.flowboard.auth.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String fullName;
    private String username;
    private String bio;
    private String avatarUrl;
}
