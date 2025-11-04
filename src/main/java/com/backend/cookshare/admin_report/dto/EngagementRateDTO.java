package com.backend.cookshare.admin_report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementRateDTO {
    private String recipeId;
    private String title;
    private Long viewCount;
    private Long engagementCount;
    private Double engagementRate;
}
