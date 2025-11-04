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
public class SearchOverviewDTO {
    Long totalSearches;              // Tổng số lượt tìm kiếm
    Long uniqueSearchQueries;        // Số từ khóa unique
    Long successfulSearches;         // Số tìm kiếm có kết quả
    Long failedSearches;             // Số tìm kiếm không có kết quả
    BigDecimal successRate;          // Tỷ lệ thành công (%)
    BigDecimal averageResultsPerSearch; // Trung bình kết quả mỗi lần tìm kiếm
    Long totalUsers;                 // Số người dùng đã tìm kiếm
    BigDecimal averageSearchesPerUser; // Trung bình tìm kiếm mỗi người
    LocalDateTime periodStart;
    LocalDateTime periodEnd;
}
