package com.backend.cookshare.admin_report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionOverviewDTO {
    private Long totalLikes;           // Tổng số lượt like
    private Long totalComments;        // Tổng số bình luận
    private Long totalSaves;           // Tổng số lượt lưu
    private Long totalRecipes;         // Tổng số công thức
    private BigDecimal engagementRate; // Tỷ lệ tương tác (%)
    private BigDecimal averageLikesPerRecipe;
    private BigDecimal averageCommentsPerRecipe;
    private BigDecimal averageSavesPerRecipe;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
