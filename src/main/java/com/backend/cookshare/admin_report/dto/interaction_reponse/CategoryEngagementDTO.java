package com.backend.cookshare.admin_report.dto.interaction_reponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryEngagementDTO {
     UUID categoryId;
     String categoryName;
     Long recipeCount;
     Long totalViews;
     Long totalLikes;
     Long totalComments;
     Long totalSaves;
     BigDecimal engagementRate;
     BigDecimal averageViewsPerRecipe;
     BigDecimal averageLikesPerRecipe;
     BigDecimal averageCommentsPerRecipe;
     BigDecimal averageSavesPerRecipe;
}
