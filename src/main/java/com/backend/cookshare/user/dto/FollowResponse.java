package com.backend.cookshare.user.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FollowResponse {
    UUID followerId;
    UUID followingId;
    String followerUsername;
    String followingUsername;
    LocalDateTime createdAt;
    String message;
}
