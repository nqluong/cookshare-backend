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
public class SearchTrendsDTO {
     List<SearchTrendDataDTO> trendData;
     BigDecimal growthRate;           // Tỷ lệ tăng trưởng (%)
     String peakPeriod;               // Thời điểm cao điểm
     LocalDateTime periodStart;
     LocalDateTime periodEnd;
}
