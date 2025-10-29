package com.backend.cookshare.user.dto;

import com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeByFollowingResponse {
    UUID followerId;
    UUID followingId;
    LocalDateTime createdAt;
    private RecipeSummaryResponse recipe;
}
