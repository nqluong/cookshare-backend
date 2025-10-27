package com.backend.cookshare.authentication.dto.response;

import com.backend.cookshare.authentication.enums.UserRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserListResponseDTO {
    UUID userId;
    String username;
    String email;
    String fullName;
    String avatarUrl;
    UserRole role;
    Boolean isActive;
    Boolean emailVerified;
    Integer followerCount;
    Integer followingCount;
    Integer recipeCount;
    LocalDateTime lastActive;
    LocalDateTime createdAt;
}

