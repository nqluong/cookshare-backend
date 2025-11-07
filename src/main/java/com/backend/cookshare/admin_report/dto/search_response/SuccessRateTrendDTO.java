package com.backend.cookshare.admin_report.dto.search_response;

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
public class SuccessRateTrendDTO {
    private LocalDateTime date;
    private Long totalSearches;
    private Long successfulSearches;
    private BigDecimal successRate;
}
