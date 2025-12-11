package com.backend.cookshare.system.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchReviewResponse {
    // ID công thức được xem xét
    UUID recipeId;
    
    // Số lượng báo cáo bị ảnh hưởng
    Integer totalReportsAffected;
    
    // Hành động đã thực hiện
    String actionTaken;
    
    // Trạng thái sau xem xét
    String status;

    // Danh sách các báo cáo đã được xem xét
    List<UUID> reviewedReportIds;

    // Thông báo
    String message;
}
