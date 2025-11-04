package com.backend.cookshare.user.controller;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.user.dto.CommentRequest;
import com.backend.cookshare.user.dto.CommentResponse;
import com.backend.cookshare.user.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách comment gốc của một recipe
     * GET /api/comments/recipe/{recipeId}?page=0&size=10
     */
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<Page<CommentResponse>> getRecipeComments(
            @PathVariable UUID recipeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<CommentResponse> comments = commentService.getRecipeComments(recipeId, page, size);
        return ResponseEntity.ok(comments);
    }

    /**
     * Lấy danh sách reply của một comment
     * GET /api/comments/{commentId}/replies
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentResponse>> getCommentReplies(@PathVariable UUID commentId) {
        List<CommentResponse> replies = commentService.getCommentReplies(commentId);
        return ResponseEntity.ok(replies);
    }

    /**
     * Tạo comment mới
     * POST /api/comments
     */
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @RequestBody CommentRequest request,
            Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UUID userId = user.getUserId();
        CommentResponse comment = commentService.createComment(request, userId);
        return ResponseEntity.ok(comment);
    }

    /**
     * Cập nhật comment
     * PUT /api/comments/{commentId}
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @RequestBody CommentRequest request,
            Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UUID userId = user.getUserId();
        CommentResponse comment = commentService.updateComment(commentId, request, userId);
        return ResponseEntity.ok(comment);
    }

    /**
     * Xóa comment
     * DELETE /api/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        UUID userId = user.getUserId();
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

}