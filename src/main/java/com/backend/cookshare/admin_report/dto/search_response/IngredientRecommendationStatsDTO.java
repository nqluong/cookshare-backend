package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngredientRecommendationStatsDTO {
     String ingredientId;
     String ingredientName;
     Long searchCount;
     Long recommendationShown;
     Long recommendationClicked;
     BigDecimal clickThroughRate;
     BigDecimal accuracy;
}
