package com.backend.cookshare.admin_report.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedInteractionStatsDTO {
    private BigDecimal averageLikesPerRecipe;
    private BigDecimal averageCommentsPerRecipe;
    private BigDecimal averageSavesPerRecipe;
    private BigDecimal medianLikesPerRecipe;
    private BigDecimal medianCommentsPerRecipe;
    private BigDecimal medianSavesPerRecipe;
    private Long maxLikesOnRecipe;
    private Long maxCommentsOnRecipe;
    private Long maxSavesOnRecipe;
    private InteractionDistribution likeDistribution;
    private InteractionDistribution commentDistribution;
    private InteractionDistribution saveDistribution;
}
