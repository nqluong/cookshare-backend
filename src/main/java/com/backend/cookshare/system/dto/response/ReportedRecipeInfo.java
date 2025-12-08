package com.backend.cookshare.system.dto.response;

import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportedRecipeInfo {
     UUID recipeId;
     String title;
     String slug;
     String featuredImage;
     RecipeStatus status;
     Boolean isPublished;
     Integer viewCount;
     UUID userId;
     String authorUsername;
}
