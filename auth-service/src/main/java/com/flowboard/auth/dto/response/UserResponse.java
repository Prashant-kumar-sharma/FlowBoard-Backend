package com.flowboard.auth.dto.response;

import com.flowboard.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String username;
    private String avatarUrl;
    private String bio;
    private User.Role role;
    private User.AuthProvider provider;
    private Boolean isActive;
    private Boolean premium;
    private String planCode;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .provider(user.getProvider())
                .isActive(user.getIsActive())
                .premium(false)
                .planCode("FREE")
                .createdAt(user.getCreatedAt())
                .build();
    }
}
