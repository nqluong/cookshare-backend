package com.backend.cookshare.admin_report.repository.interaction_projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TopCommentProjection {
    UUID getCommentId();
    String getContent();
    UUID getRecipeId();
    String getRecipeTitle();
    UUID getUserId();
    String getUsername();
    String getAvatarUrl();
    Long getLikeCount();
    LocalDateTime getCreatedAt();
}
