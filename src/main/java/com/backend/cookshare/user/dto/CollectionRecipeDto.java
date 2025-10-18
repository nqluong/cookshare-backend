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
public class CollectionRecipeDto {
    UUID recipeId;
    String title;
    String slug;
    String description;
    Integer prepTime;
    Integer cookTime;
    Integer servings;
    String difficulty;
    String featuredImage;
    Integer viewCount;
    Integer saveCount;
    Integer likeCount;
    String averageRating;
//    LocalDateTime createdAt;
//    LocalDateTime addedAt;
    // Thời điểm thêm vào collection
}