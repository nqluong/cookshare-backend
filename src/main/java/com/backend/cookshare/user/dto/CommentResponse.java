package com.backend.cookshare.user.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private UUID commentId;
    private UUID recipeId;
    private UUID userId;
    private String userName;
    private String userAvatar;
    private String content;
    private UUID parentCommentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer replyCount;
    private List<CommentResponse> replies= new ArrayList<>();
}