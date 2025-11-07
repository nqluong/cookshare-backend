package com.backend.cookshare.admin_report.dto.interaction_reponse;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentDetailDTO {
    UUID commentId;
    String content;
    UUID recipeId;
    String recipeTitle;
    UUID userId;
    String username;
    String userAvatar;
    Long likeCount;
    LocalDateTime createdAt;
}
