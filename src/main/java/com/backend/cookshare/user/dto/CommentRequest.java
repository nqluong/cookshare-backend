package com.backend.cookshare.user.dto;

import lombok.*;
import java.util.UUID;

// DTO cho request tạo/cập nhật comment
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    private UUID recipeId;
    private String content;
    private UUID parentCommentId;
}
