package com.backend.cookshare.recipe_management.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO cho yêu cầu phê duyệt công thức
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminRecipeApprovalRequest {
    Boolean approved; // true: phê duyệt, false: từ chối
    String adminNote; // Ghi chú của admin (có thể null)
    String rejectionReason; // Lý do từ chối (nếu approved = false)
}
