package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.dto.CommentRequest;
import com.backend.cookshare.user.dto.CommentResponse;
import com.backend.cookshare.user.dto.CommentWebSocketMessage;
import com.backend.cookshare.user.entity.Comment;
import com.backend.cookshare.user.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private CommentService commentService;

    private UUID recipeId;
    private UUID userId;
    private UUID commentId;
    private User user;
    private Recipe recipe;
    private Comment comment;

    @BeforeEach
    void setUp() {
        recipeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        commentId = UUID.randomUUID();

        user = new User();
        user.setUserId(userId);
        user.setUsername("testuser");
        user.setFullName("Test User");
        user.setAvatarUrl("avatar.jpg");

        recipe = new Recipe();
        recipe.setRecipeId(recipeId);
        recipe.setUserId(userId);

        comment = Comment.builder()
                .commentId(commentId)
                .recipeId(recipeId)
                .userId(userId)
                .content("Test comment")
                .parentCommentId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        comment.setUser(user);
    }

    @Test
    void getRecipeComments_ShouldReturnPageOfComments() {
        // Arrange
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<Comment> commentList = Arrays.asList(comment);
        Page<Comment> commentPage = new PageImpl<>(commentList, pageable, 1);

        when(commentRepository.findRootCommentsByRecipeId(recipeId, pageable))
                .thenReturn(commentPage);
        when(commentRepository.countRepliesByParentCommentId(commentId))
                .thenReturn(0);
        when(commentRepository.findRepliesByParentCommentId(commentId))
                .thenReturn(Collections.emptyList());

        // Act
        Page<CommentResponse> result = commentService.getRecipeComments(recipeId, page, size);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(commentId, result.getContent().get(0).getCommentId());
        verify(commentRepository).findRootCommentsByRecipeId(recipeId, pageable);
    }

    @Test
    void getCommentReplies_ShouldReturnListOfReplies() {
        // Arrange
        Comment reply = Comment.builder()
                .commentId(UUID.randomUUID())
                .recipeId(recipeId)
                .userId(userId)
                .content("Reply comment")
                .parentCommentId(commentId)
                .build();
        reply.setUser(user);

        when(commentRepository.findRepliesByParentCommentId(commentId))
                .thenReturn(Arrays.asList(reply));
        when(commentRepository.countRepliesByParentCommentId(any()))
                .thenReturn(0);
        when(commentRepository.findRepliesByParentCommentId(reply.getCommentId()))
                .thenReturn(Collections.emptyList());

        // Act
        List<CommentResponse> result = commentService.getCommentReplies(commentId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(commentId, result.get(0).getParentCommentId());
        verify(commentRepository).findRepliesByParentCommentId(commentId);
    }

    @Test
    void createComment_RootComment_ShouldCreateSuccessfully() {
        // Arrange
        UUID recipeOwnerId = UUID.randomUUID();
        recipe.setUserId(recipeOwnerId);

        CommentRequest request = new CommentRequest();
        request.setRecipeId(recipeId);
        request.setContent("New comment");
        request.setParentCommentId(null);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.countRepliesByParentCommentId(commentId)).thenReturn(0);
        when(commentRepository.findRepliesByParentCommentId(commentId))
                .thenReturn(Collections.emptyList());
        doNothing().when(notificationService).createCommentNotification(any(), any(), any(), any());

        // Act
        CommentResponse result = commentService.createComment(request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(commentId, result.getCommentId());
        verify(recipeRepository).findById(recipeId);
        verify(commentRepository).save(any(Comment.class));
        verify(notificationService).createCommentNotification(recipeOwnerId, userId, commentId, recipeId);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/recipe/" + recipeId + "/comments"),
                any(CommentWebSocketMessage.class)
        );
    }

    @Test
    void createComment_RootCommentByRecipeOwner_ShouldNotCreateNotification() {
        // Arrange
        recipe.setUserId(userId); // Same as commenter

        CommentRequest request = new CommentRequest();
        request.setRecipeId(recipeId);
        request.setContent("New comment");
        request.setParentCommentId(null);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.countRepliesByParentCommentId(commentId)).thenReturn(0);
        when(commentRepository.findRepliesByParentCommentId(commentId))
                .thenReturn(Collections.emptyList());

        // Act
        CommentResponse result = commentService.createComment(request, userId);

        // Assert
        assertNotNull(result);
        verify(notificationService, never()).createCommentNotification(any(), any(), any(), any());
    }

    @Test
    void createComment_ReplyComment_ShouldCreateReplyNotification() {
        // Arrange
        UUID parentCommentId = UUID.randomUUID();
        UUID parentCommentUserId = UUID.randomUUID();

        Comment parentComment = Comment.builder()
                .commentId(parentCommentId)
                .recipeId(recipeId)
                .userId(parentCommentUserId)
                .content("Parent comment")
                .build();

        CommentRequest request = new CommentRequest();
        request.setRecipeId(recipeId);
        request.setContent("Reply comment");
        request.setParentCommentId(parentCommentId);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.countRepliesByParentCommentId(commentId)).thenReturn(0);
        when(commentRepository.findRepliesByParentCommentId(commentId))
                .thenReturn(Collections.emptyList());
        doNothing().when(notificationService).createCommentReplyNotification(any(), any(), any(), any());

        // Act
        CommentResponse result = commentService.createComment(request, userId);

        // Assert
        assertNotNull(result);
        verify(notificationService).createCommentReplyNotification(
                parentCommentUserId, userId, commentId, recipeId
        );
        verify(notificationService, never()).createCommentNotification(any(), any(), any(), any());
    }

    @Test
    void createComment_RecipeNotFound_ShouldThrowException() {
        // Arrange
        CommentRequest request = new CommentRequest();
        request.setRecipeId(recipeId);
        request.setContent("New comment");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.createComment(request, userId);
        });

        assertEquals("Recipe not found", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_ParentCommentNotFound_ShouldThrowException() {
        // Arrange
        UUID parentCommentId = UUID.randomUUID();
        CommentRequest request = new CommentRequest();
        request.setRecipeId(recipeId);
        request.setContent("Reply comment");
        request.setParentCommentId(parentCommentId);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.createComment(request, userId);
        });

        assertEquals("Parent comment not found", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_ShouldUpdateSuccessfully() {
        // Arrange
        CommentRequest request = new CommentRequest();
        request.setContent("Updated content");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(commentRepository.countRepliesByParentCommentId(commentId)).thenReturn(0);
        when(commentRepository.findRepliesByParentCommentId(commentId))
                .thenReturn(Collections.emptyList());

        // Act
        CommentResponse result = commentService.updateComment(commentId, request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(commentId, result.getCommentId());
        verify(commentRepository).save(any(Comment.class));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/recipe/" + recipeId + "/comments"),
                any(CommentWebSocketMessage.class)
        );
    }

    @Test
    void updateComment_NotOwner_ShouldThrowException() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        CommentRequest request = new CommentRequest();
        request.setContent("Updated content");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.updateComment(commentId, request, differentUserId);
        });

        assertEquals("You can only update your own comments", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_CommentNotFound_ShouldThrowException() {
        // Arrange
        CommentRequest request = new CommentRequest();
        request.setContent("Updated content");

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.updateComment(commentId, request, userId);
        });

        assertEquals("Comment not found", exception.getMessage());
    }

    @Test
    void updateComment_UserNotFoundInRepo_ShouldStillUpdate() {
        // Arrange
        CommentRequest request = new CommentRequest();
        request.setContent("Updated content");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(commentRepository.countRepliesByParentCommentId(commentId)).thenReturn(0);
        when(commentRepository.findRepliesByParentCommentId(commentId))
                .thenReturn(Collections.emptyList());

        // Act
        CommentResponse result = commentService.updateComment(commentId, request, userId);

        // Assert
        assertNotNull(result);
        assertNull(result.getUserName());
    }

    @Test
    void deleteComment_RootCommentWithReplies_ShouldDeleteAllReplies() {
        // Arrange
        UUID replyId1 = UUID.randomUUID();
        UUID replyId2 = UUID.randomUUID();

        Comment reply1 = Comment.builder()
                .commentId(replyId1)
                .recipeId(recipeId)
                .userId(userId)
                .content("Reply 1")
                .parentCommentId(commentId)
                .build();

        Comment reply2 = Comment.builder()
                .commentId(replyId2)
                .recipeId(recipeId)
                .userId(userId)
                .content("Reply 2")
                .parentCommentId(commentId)
                .build();

        List<Comment> replies = Arrays.asList(reply1, reply2);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.findRepliesByParentCommentId(commentId)).thenReturn(replies);
        doNothing().when(notificationService).deleteCommentNotifications(commentId, userId);
        doNothing().when(notificationService).deleteReplyNotifications(anyList(), eq(userId));
        doNothing().when(commentRepository).deleteAll(replies);
        doNothing().when(commentRepository).delete(comment);

        // Act
        commentService.deleteComment(commentId, userId);

        // Assert
        verify(commentRepository).findRepliesByParentCommentId(commentId);
        verify(notificationService).deleteCommentNotifications(commentId, userId);
        verify(notificationService).deleteReplyNotifications(anyList(), eq(userId));
        verify(commentRepository).deleteAll(replies);
        verify(commentRepository).delete(comment);
        verify(messagingTemplate, times(3)).convertAndSend(
                eq("/topic/recipe/" + recipeId + "/comments"),
                any(CommentWebSocketMessage.class)
        );
    }

    @Test
    void deleteComment_ReplyComment_ShouldNotDeleteOtherReplies() {
        // Arrange
        UUID parentCommentId = UUID.randomUUID();
        comment.setParentCommentId(parentCommentId);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        doNothing().when(notificationService).deleteCommentNotifications(commentId, userId);
        doNothing().when(commentRepository).delete(comment);

        // Act
        commentService.deleteComment(commentId, userId);

        // Assert
        verify(commentRepository, never()).findRepliesByParentCommentId(any());
        verify(notificationService).deleteCommentNotifications(commentId, userId);
        verify(notificationService, never()).deleteReplyNotifications(anyList(), any());
        verify(commentRepository).delete(comment);
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/recipe/" + recipeId + "/comments"),
                any(CommentWebSocketMessage.class)
        );
    }

    @Test
    void deleteComment_NotOwner_ShouldThrowException() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.deleteComment(commentId, differentUserId);
        });

        assertEquals("You can only delete your own comments", exception.getMessage());
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_CommentNotFound_ShouldThrowException() {
        // Arrange
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.deleteComment(commentId, userId);
        });

        assertEquals("Comment not found", exception.getMessage());
    }

    @Test
    void deleteComment_RootCommentWithNoReplies_ShouldDeleteOnlyComment() {
        // Arrange
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.findRepliesByParentCommentId(commentId))
                .thenReturn(Collections.emptyList());
        doNothing().when(notificationService).deleteCommentNotifications(commentId, userId);
        doNothing().when(commentRepository).delete(comment);

        // Act
        commentService.deleteComment(commentId, userId);

        // Assert
        verify(commentRepository).findRepliesByParentCommentId(commentId);
        verify(commentRepository).deleteAll(Collections.emptyList());
        verify(commentRepository).delete(comment);
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/recipe/" + recipeId + "/comments"),
                any(CommentWebSocketMessage.class)
        );
    }

    @Test
    void createComment_UserNotFound_ShouldThrowException() {
        // Arrange
        CommentRequest request = new CommentRequest();
        request.setRecipeId(recipeId);
        request.setContent("New comment");

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            commentService.createComment(request, userId);
        });

        assertEquals("User not found", exception.getMessage());
    }
}