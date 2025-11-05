package com.backend.cookshare.authentication.dto.response;

import com.backend.cookshare.authentication.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // thời gian sống của token (seconds)

    // Thông tin user
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private UUID userId;
        private String username;
        private String email;
        private String fullName;
        private String avatarUrl;
        private String bio;
        private UserRole role;
        private Boolean isActive;
        private Boolean emailVerified;
    }
}
