package com.backend.cookshare.interaction.entity.dto.response;
import com.backend.cookshare.recipe_management.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSummaryResponse {
    private UUID recipeId;
    private String title;
    private String slug;
    private String description;
    private String featuredImage;
    // Thông tin thời gian và độ khó
    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
    private Difficulty difficulty;

    // Thông tin người tạo
    private UUID userId;
    private String userName;

    // Số liệu tương tác
    private Integer viewCount;
    private Integer saveCount;
    private Integer likeCount;
    private BigDecimal averageRating;
    private Integer ratingCount;

    private Boolean isFeatured;
    private Boolean isPublished;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
