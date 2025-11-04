package com.backend.cookshare.admin_report.dto.search_response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryViewStatsDTO {
    private String categoryId;
    private String categoryName;
    private Long viewCount;                // Tổng lượt xem
    private Long uniqueUsers;              // Số người xem unique
    private Long recipeCount;              // Số công thức trong danh mục
    private BigDecimal averageTimeSpent;   // Thời gian trung bình (giây)
    private BigDecimal clickThroughRate;   // Tỷ lệ click vào công thức (%)
    private BigDecimal viewShare;          // % trên tổng lượt xem
}
