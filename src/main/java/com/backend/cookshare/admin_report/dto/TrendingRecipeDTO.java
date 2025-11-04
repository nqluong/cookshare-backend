package com.backend.cookshare.admin_report.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TrendingRecipeDTO {
     String recipeId;
     String title;
     String slug;
     Long viewCount;
     Long likeCount;
     Double trendingScore;
     Double growthRate;
     LocalDateTime createdAt;
}
