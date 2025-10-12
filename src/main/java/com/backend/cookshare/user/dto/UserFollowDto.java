package com.backend.cookshare.user.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFollowDto {
    UUID userId;
    String username;
    String fullName;
    String avatarUrl;
    String bio;
    Integer followerCount;
    Integer followingCount;
    Boolean isFollowing;
}
