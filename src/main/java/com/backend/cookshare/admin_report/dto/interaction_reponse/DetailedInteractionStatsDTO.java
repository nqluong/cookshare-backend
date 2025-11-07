package com.backend.cookshare.admin_report.dto.interaction_reponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DetailedInteractionStatsDTO {
    BigDecimal averageLikesPerRecipe;
    BigDecimal averageCommentsPerRecipe;
    BigDecimal averageSavesPerRecipe;
    BigDecimal medianLikesPerRecipe;
    BigDecimal medianCommentsPerRecipe;
    BigDecimal medianSavesPerRecipe;
    Long maxLikesOnRecipe;
    Long maxCommentsOnRecipe;
    Long maxSavesOnRecipe;
    InteractionDistribution likeDistribution;
    InteractionDistribution commentDistribution;
    InteractionDistribution saveDistribution;
}
