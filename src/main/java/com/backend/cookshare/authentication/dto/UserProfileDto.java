package com.backend.cookshare.authentication.dto;

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
public class UserProfileDto {
    private UUID userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private UserRole role;
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime lastActive;
    private Integer followerCount;
    private Integer followingCount;
    private Integer recipeCount;
    private Integer totalLikes;
    private LocalDateTime createdAt;
}
