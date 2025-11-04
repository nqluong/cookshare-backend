package com.backend.cookshare.admin_report.dto.search_response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuccessRateDTO {
    private Long totalSearches;
    private Long successfulSearches;        // Có kết quả
    private Long failedSearches;            // Không có kết quả
    private BigDecimal successRate;         // % thành công
    private BigDecimal failureRate;         // % thất bại
    private List<SuccessRateByTypeDTO> successByType; // Theo loại tìm kiếm
    private List<SuccessRateTrendDTO> trendData; // Xu hướng theo thời gian
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}
