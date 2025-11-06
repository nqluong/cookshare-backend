package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.dto.*;
import com.backend.cookshare.user.entity.Comment;
import com.backend.cookshare.user.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    // Lấy danh sách comment gốc của recipe
    public Page<CommentResponse> getRecipeComments(UUID recipeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepository.findRootCommentsByRecipeId(recipeId, pageable);

        return comments.map(this::convertToResponse);
    }

    // Lấy danh sách reply của comment
    public List<CommentResponse> getCommentReplies(UUID commentId) {
        List<Comment> replies = commentRepository.findRepliesByParentCommentId(commentId);
        return replies.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Thêm comment mới
    @Transactional
    public CommentResponse createComment(CommentRequest request, UUID userId) {
        // Validate recipe exists
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        // Validate parent comment if exists
        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findById(request.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
        }

        // Create comment
        Comment comment = Comment.builder()
                .recipeId(request.getRecipeId())
                .userId(userId)
                .content(request.getContent())
                .parentCommentId(request.getParentCommentId())
                .build();

        comment = commentRepository.save(comment);

        // Load user info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        comment.setUser(user);

        CommentResponse response = convertToResponse(comment);

        // Send WebSocket message to all users viewing this recipe
        sendCommentWebSocketMessage("CREATE", response, request.getRecipeId());

        // Create notification
        if (parentComment != null) {
            // Notify parent comment owner
            notificationService.createCommentNotification(
                    parentComment.getUserId(),
                    userId,
                    comment.getCommentId(),
                    request.getRecipeId(),
                    true
            );
        } else {
            // Notify recipe owner
            if (!recipe.getUserId().equals(userId)) {
                notificationService.createCommentNotification(
                        recipe.getUserId(),
                        userId,
                        comment.getCommentId(),
                        request.getRecipeId(),
                        false
                );
            }
        }

        return response;
    }

    // Cập nhật comment
    @Transactional
    public CommentResponse updateComment(UUID commentId, CommentRequest request, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Check ownership
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only update your own comments");
        }

        comment.setContent(request.getContent());
        comment = commentRepository.save(comment);

        // Load user info
        User user = userRepository.findById(userId).orElse(null);
        comment.setUser(user);

        CommentResponse response = convertToResponse(comment);

        // Send WebSocket message
        sendCommentWebSocketMessage("UPDATE", response, comment.getRecipeId());

        return response;
    }

    // Xóa comment
    @Transactional
    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Check ownership
        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own comments");
        }

        UUID recipeId = comment.getRecipeId();

        // CASCADE DELETE: Xóa tất cả replies trước
        if (comment.getParentCommentId() == null) {
            // Đây là comment cha, xóa tất cả replies
            List<Comment> replies = commentRepository.findRepliesByParentCommentId(commentId);

            // Send WebSocket message cho từng reply bị xóa
            for (Comment reply : replies) {
                CommentResponse replyResponse = CommentResponse.builder()
                        .commentId(reply.getCommentId())
                        .recipeId(recipeId)
                        .parentCommentId(commentId)
                        .build();
                sendCommentWebSocketMessage("DELETE", replyResponse, recipeId);
            }

            // Xóa tất cả replies
            commentRepository.deleteAll(replies);
        }

        // Xóa comment chính
        commentRepository.delete(comment);

        // Create a minimal response for deletion
        CommentResponse response = CommentResponse.builder()
                .commentId(commentId)
                .recipeId(recipeId)
                .parentCommentId(comment.getParentCommentId())
                .build();

        // Send WebSocket message
        sendCommentWebSocketMessage("DELETE", response, recipeId);
    }

    // Convert entity to DTO
    private CommentResponse convertToResponse(Comment comment) {
        Integer replyCount = commentRepository.countRepliesByParentCommentId(comment.getCommentId());

        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .recipeId(comment.getRecipeId())
                .userId(comment.getUserId())
                .userName(comment.getUser() != null ? comment.getUser().getUsername() : null)
                .userAvatar(comment.getUser() != null ? comment.getUser().getAvatarUrl() : null)
                .content(comment.getContent())
                .parentCommentId(comment.getParentCommentId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replyCount(replyCount)
                .replies(this.getCommentReplies(comment.getCommentId()))
                .build();
    }

    // Send WebSocket message for comment
    private void sendCommentWebSocketMessage(String action, CommentResponse comment, UUID recipeId) {
        CommentWebSocketMessage message = CommentWebSocketMessage.builder()
                .action(action)
                .comment(comment)
                .recipeId(recipeId)
                .timestamp(LocalDateTime.now())
                .build();

        // Send to all users subscribed to this recipe's comments
        messagingTemplate.convertAndSend("/topic/recipe/" + recipeId + "/comments", message);
    }
}