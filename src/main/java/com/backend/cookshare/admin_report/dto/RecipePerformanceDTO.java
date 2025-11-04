package com.backend.cookshare.admin_report.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipePerformanceDTO {
     String recipeId;
     String title;
     String slug;
     Long viewCount;
     Long likeCount;
     Long saveCount;
     Long commentCount;
     BigDecimal averageRating;
     Long ratingCount;
     String authorName;
     LocalDateTime createdAt;
     Double trendingScore;
}
