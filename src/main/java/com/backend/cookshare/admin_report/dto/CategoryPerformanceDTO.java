package com.backend.cookshare.admin_report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPerformanceDTO {
    private String categoryName;
    private Long recipeCount;
    private Long totalViews;
    private Long totalLikes;
    private Double avgRating;
    private Double avgEngagementRate;
}
