package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecommendationEffectivenessDTO {
    private Long totalRecommendationShown;   // Tổng số lần hiển thị gợi ý
    private Long totalRecommendationClicked; // Tổng số lần click
    private BigDecimal clickThroughRate;     // Tỷ lệ click (%)
    private BigDecimal averagePosition;      // Vị trí trung bình được click
    private List<RecommendationTypeStatsDTO> byType; // Theo loại gợi ý
    private List<RecommendationSourceStatsDTO> bySource; // Theo nguồn gợi ý
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}