package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationSourceStatsDTO {
    private String source;                   // "HOME", "SEARCH", "RECIPE_DETAIL", "CATEGORY"
    private Long shownCount;
    private Long clickedCount;
    private BigDecimal clickThroughRate;
}
