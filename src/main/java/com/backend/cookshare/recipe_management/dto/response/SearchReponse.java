package com.backend.cookshare.recipe_management.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchReponse {
    UUID recipeId;
    UUID userId;
    String title;
    String slug;
    String fullName;
    String avatarUrl;
    String featuredImage;
    Integer cookTime;
    Integer viewCount = 0;
    Integer likeCount = 0;
    Integer saveCount = 0;
}
