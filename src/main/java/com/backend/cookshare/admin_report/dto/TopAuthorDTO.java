package com.backend.cookshare.admin_report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopAuthorDTO {
    private String userId;
    private String authorName;
    private String username;
    private Long recipeCount;
    private Long totalViews;
    private Long totalLikes;
    private Double avgRating;
}
