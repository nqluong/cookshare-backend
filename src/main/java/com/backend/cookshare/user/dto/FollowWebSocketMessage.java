package com.backend.cookshare.user.dto;

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
public class FollowWebSocketMessage {
    private String action;
    private UUID followerId;
    private UUID followingId;
    private String followerUsername;
    private String followerFullName;
    private String followerAvatarUrl;
    private Integer followerCount;
    private Integer followingCount;
    private LocalDateTime timestamp;
}