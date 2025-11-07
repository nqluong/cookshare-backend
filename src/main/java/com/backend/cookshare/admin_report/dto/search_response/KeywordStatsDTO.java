package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeywordStatsDTO {
     String keyword;
     Long searchCount;              // Số lần tìm kiếm
     Long uniqueUsers;              // Số người dùng unique
     BigDecimal averageResults;     // Trung bình kết quả
     BigDecimal successRate;        // Tỷ lệ thành công (%)
     LocalDateTime lastSearched;    // Lần tìm cuối
     String trend;                  // "UP", "DOWN", "STABLE"
}
