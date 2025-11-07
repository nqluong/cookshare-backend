package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationTypeStatsDTO {
    private String recommendationType;       // "POPULAR", "PERSONALIZED", "SIMILAR", "TRENDING"
    private Long shownCount;
    private Long clickedCount;
    private BigDecimal clickThroughRate;
    private BigDecimal conversionRate;       // Tỷ lệ chuyển đổi (xem -> tương tác)
}
