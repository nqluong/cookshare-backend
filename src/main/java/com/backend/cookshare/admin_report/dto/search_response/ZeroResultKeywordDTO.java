package com.backend.cookshare.admin_report.dto.search_response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZeroResultKeywordDTO {
    private String keyword;
    private Long searchCount;
    private Long uniqueUsers;
    private LocalDateTime firstSearched;
    private LocalDateTime lastSearched;
    private List<String> suggestedActions;  // Hành động đề xuất (tạo nội dung, sửa lỗi chính tả...)
}
