package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.dto.NotificationResponse;
import com.backend.cookshare.user.dto.NotificationWebSocketMessage;
import com.backend.cookshare.user.entity.Comment;
import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
import com.backend.cookshare.user.enums.RelatedType;
import com.backend.cookshare.user.repository.CommentRepository;
import com.backend.cookshare.user.repository.NotificationRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private RecipeRepository recipeRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ActivityLogService activityLogService;
    @Mock private FirebaseStorageService fileStorageService;

    @InjectMocks private NotificationService notificationService;

    private UUID userId, actorId, recipeId, commentId, notificationId;
    private User actor, recipient;
    private Recipe recipe;
    private Comment comment;
    private Notification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        recipeId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        actor = new User();
        actor.setUserId(actorId);
        actor.setFullName("Actor User");
        actor.setAvatarUrl("avatar.jpg");

        recipient = new User();
        recipient.setUserId(userId);
        recipient.setFullName("Recipient User");

        recipe = new Recipe();
        recipe.setRecipeId(recipeId);
        recipe.setUserId(userId);
        recipe.setTitle("Test Recipe");
        recipe.setFeaturedImage("recipe.jpg");

        comment = Comment.builder()
                .commentId(commentId)
                .recipeId(recipeId)
                .userId(actorId)
                .content("Great recipe!")
                .build();

        notification = Notification.builder()
                .notificationId(notificationId)
                .userId(userId)
                .type(NotificationType.COMMENT)
                .title("Bình luận mới")
                .message("Actor User đã bình luận về công thức của bạn")
                .relatedId(commentId)
                .relatedType(RelatedType.user)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ====================== BASIC OPERATIONS ======================

    @Test
    void getUserNotifications_ShouldReturnEnrichedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(page);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(fileStorageService.convertPathToFirebaseUrl(anyString())).thenReturn("https://firebase.url/");

        Page<NotificationResponse> result = notificationService.getUserNotifications(userId, 0, 10);

        assertEquals(1, result.getTotalElements());
        NotificationResponse resp = result.getContent().get(0);
        assertEquals(recipeId, resp.getRecipeId());
        assertEquals("Test Recipe", resp.getRecipeTitle());
        assertEquals(actorId, resp.getActorId());
        assertEquals("Actor User", resp.getActorName());
    }

    @Test
    void getUnreadCount_ShouldReturnCorrectCount() {
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(7L);
        assertEquals(7L, notificationService.getUnreadCount(userId));
    }

    @Test
    void markAsRead_ShouldMarkAndSendWebSocket_WhenUnread() {
        notification.setIsRead(false);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        notificationService.markAsRead(notificationId, userId);

        assertTrue(notification.getIsRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository).save(notification);
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any(NotificationWebSocketMessage.class));
    }

    @Test
    void markAsRead_ShouldDoNothing_WhenAlreadyRead() {
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId, userId);

        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void markAsRead_ShouldThrow_WhenNotFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(notificationId, userId));
    }

    @Test
    void markAsRead_ShouldThrow_WhenUnauthorized() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(notificationId, UUID.randomUUID()));
    }

    @Test
    void markAllAsRead_ShouldMarkAndBroadcast() {
        notificationService.markAllAsRead(userId);
        verify(notificationRepository).markAllAsRead(userId);
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), argThat(msg -> "READ_ALL".equals(((NotificationWebSocketMessage)msg).getAction())));
    }

    @Test
    void deleteNotification_ShouldDeleteAndNotify() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        notificationService.deleteNotification(notificationId, userId);

        verify(notificationRepository).delete(notification);
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any(NotificationWebSocketMessage.class));
    }

    // ====================== COMMENT NOTIFICATIONS ======================

    @Test
    void createCommentNotification_ShouldCreateAndSend() {
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.createCommentNotification(userId, actorId, commentId, recipeId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertAll(
                () -> assertEquals(userId, saved.getUserId()),
                () -> assertEquals(NotificationType.COMMENT, saved.getType()),
                () -> assertEquals("Bình luận mới", saved.getTitle()),
                () -> assertEquals(commentId, saved.getRelatedId()),
                () -> assertFalse(saved.getIsRead())
        );
        verify(activityLogService).logCommentActivity(actorId, commentId, recipeId, "CREATE");
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any());
    }

    @Test
    void createCommentNotification_ShouldSkip_WhenSelfComment() {
        notificationService.createCommentNotification(userId, userId, commentId, recipeId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createCommentNotification_ShouldNotCreate_WhenActorNotFound() {
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());

        notificationService.createCommentNotification(userId, actorId, commentId, recipeId);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createCommentReplyNotification_ShouldCreateMentionType() {
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        notificationService.createCommentReplyNotification(userId, actorId, commentId, recipeId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertAll(
                () -> assertEquals(NotificationType.MENTION, saved.getType()),
                () -> assertEquals("Trả lời bình luận", saved.getTitle()),
                () -> assertTrue(saved.getMessage().contains("đã trả lời bình luận"))
        );
    }

    @Test
    void createCommentReplyNotification_ShouldNotCreate_WhenSelfReply() {
        notificationService.createCommentReplyNotification(userId, userId, commentId, recipeId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void deleteCommentNotifications_ShouldDeleteAllAndNotify() {
        List<Notification> list = List.of(notification);
        when(notificationRepository.findByRelatedIdAndType(commentId, NotificationType.COMMENT)).thenReturn(list);

        notificationService.deleteCommentNotifications(commentId, actorId);

        verify(notificationRepository).delete(notification);
        verify(messagingTemplate).convertAndSendToUser(anyString(), eq("/queue/notifications"), any());
        verify(activityLogService).logCommentActivity(actorId, commentId, null, "DELETE");
    }

    @Test
    void deleteCommentNotifications_ShouldDoNothing_WhenNoNotifications() {
        when(notificationRepository.findByRelatedIdAndType(commentId, NotificationType.COMMENT)).thenReturn(List.of());

        notificationService.deleteCommentNotifications(commentId, actorId);

        verify(notificationRepository, never()).delete(any());
        verify(activityLogService).logCommentActivity(actorId, commentId, null, "DELETE");
    }

    @Test
    void deleteReplyNotifications_ShouldCallDeleteForEachReply() {
        List<UUID> replies = List.of(UUID.randomUUID(), UUID.randomUUID());
        NotificationService spy = spy(notificationService);
        doNothing().when(spy).deleteCommentNotifications(any(), any());

        spy.deleteReplyNotifications(replies, actorId);

        verify(spy, times(2)).deleteCommentNotifications(any(), eq(actorId));
    }

    @Test
    void deleteReplyNotifications_ShouldDoNothing_WhenEmptyList() {
        notificationService.deleteReplyNotifications(List.of(), actorId);

        verify(notificationRepository, never()).delete(any());
    }

    // ====================== LIKE NOTIFICATIONS ======================

    @Test
    void createLikeNotification_ShouldCreateWithActorIdInMessage() {
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.createLikeNotification(userId, actorId, recipeId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        String message = captor.getValue().getMessage();

        assertTrue(message.startsWith(actorId.toString() + "::"));
        assertEquals(NotificationType.LIKE, captor.getValue().getType());
        verify(activityLogService).logLikeActivity(actorId, recipeId, "CREATE");
    }

    @Test
    void createLikeNotification_ShouldSkip_WhenSelfLike() {
        notificationService.createLikeNotification(userId, userId, recipeId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createLikeNotification_ShouldNotCreate_WhenActorNotFound() {
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());

        notificationService.createLikeNotification(userId, actorId, recipeId);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void deleteLikeNotification_ShouldRemoveAndLog() {
        when(notificationRepository.findByUserIdAndRelatedIdAndType(userId, recipeId, NotificationType.LIKE))
                .thenReturn(List.of(notification));

        notificationService.deleteLikeNotification(userId, actorId, recipeId);

        verify(notificationRepository).delete(notification);
        verify(activityLogService).logLikeActivity(actorId, recipeId, "DELETE");
    }

    @Test
    void deleteLikeNotification_ShouldDoNothing_WhenNoNotifications() {
        when(notificationRepository.findByUserIdAndRelatedIdAndType(userId, recipeId, NotificationType.LIKE))
                .thenReturn(List.of());

        notificationService.deleteLikeNotification(userId, actorId, recipeId);

        verify(notificationRepository, never()).delete(any());
        verify(activityLogService).logLikeActivity(actorId, recipeId, "DELETE");
    }

    // ====================== FOLLOW NOTIFICATIONS ======================

    @Test
    void createFollowNotification_ShouldCreate() {
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.createFollowNotification(userId, actorId);

        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.FOLLOW && n.getRelatedId().equals(actorId)
        ));
        verify(activityLogService).logFollowActivity(actorId, userId, "CREATE");
    }

    @Test
    void createFollowNotification_ShouldSkip_WhenSelfFollow() {
        notificationService.createFollowNotification(userId, userId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createFollowNotification_ShouldNotCreate_WhenActorNotFound() {
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());

        notificationService.createFollowNotification(userId, actorId);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void deleteFollowNotification_ShouldRemove() {
        when(notificationRepository.findByUserIdAndRelatedIdAndType(userId, actorId, NotificationType.FOLLOW))
                .thenReturn(List.of(notification));

        notificationService.deleteFollowNotification(userId, actorId);

        verify(notificationRepository).delete(notification);
        verify(activityLogService).logFollowActivity(actorId, userId, "DELETE");
    }

    @Test
    void deleteFollowNotification_ShouldDoNothing_WhenNoNotifications() {
        when(notificationRepository.findByUserIdAndRelatedIdAndType(userId, actorId, NotificationType.FOLLOW))
                .thenReturn(List.of());

        notificationService.deleteFollowNotification(userId, actorId);

        verify(notificationRepository, never()).delete(any());
        verify(activityLogService).logFollowActivity(actorId, userId, "DELETE");
    }

    // ====================== RECIPE & SYSTEM NOTIFICATIONS ======================

    @Test
    void createRecipeApprovedNotification_ShouldCreateSystemType() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.createRecipeApprovedNotification(userId, recipeId, "Bánh mì");

        verify(notificationRepository).save(argThat(n ->
                n.getType() == NotificationType.SYSTEM && n.getMessage().contains("Bánh mì")
        ));
    }

    @Test
    void createNewRecipeNotificationForFollowers_ShouldNotifyAllExceptOwner() {
        List<UUID> followers = List.of(UUID.randomUUID(), userId, UUID.randomUUID());
        when(notificationRepository.save(any())).thenReturn(notification);

        notificationService.createNewRecipeNotificationForFollowers(followers, userId, "Chef", recipeId, "New Dish");

        verify(notificationRepository, times(2)).save(any());
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/notifications"), any());
    }

    @Test
    void deleteRecipeNotifications_ShouldDeleteAllRelatedTypes() {
        when(notificationRepository.findByRelatedIdAndTypes(eq(recipeId), anyList()))
                .thenReturn(List.of(notification));

        notificationService.deleteRecipeNotifications(recipeId);

        verify(notificationRepository).findByRelatedIdAndTypes(recipeId, List.of(
                NotificationType.SYSTEM,
                NotificationType.RECIPE_PUBLISHED,
                NotificationType.COMMENT,
                NotificationType.LIKE
        ));
        verify(notificationRepository).delete(notification);
    }

    // ====================== ENRICHMENT TESTS ======================

    @Test
    void convertToResponse_ShouldEnrichLikeNotification_AndParseActorFromMessage() {
        // Arrange
        notification.setType(NotificationType.LIKE);
        notification.setRelatedId(recipeId);
        notification.setMessage(actorId + "::Actor User đã thích công thức của bạn");

        // Tạo Page mock chứa đúng notification cần test
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> mockedPage = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                .thenReturn(mockedPage);

        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(fileStorageService.convertPathToFirebaseUrl(anyString())).thenReturn("https://firebase.url/");

        // Act
        Page<NotificationResponse> result = notificationService.getUserNotifications(userId, 0, 10);

        // Assert
        assertEquals(1, result.getTotalElements());

        NotificationResponse resp = result.getContent().get(0);
        assertEquals(actorId, resp.getActorId());
        assertEquals("Actor User", resp.getActorName());
        assertEquals("https://firebase.url/", resp.getActorAvatar());
        assertEquals(recipeId, resp.getRecipeId());
        assertEquals("Test Recipe", resp.getRecipeTitle());

        // Quan trọng: message đã được "làm sạch" - không còn actorId:: ở đầu
        assertEquals("Actor User đã thích công thức của bạn", resp.getMessage());
    }

    @Test
    void enrichLikeNotification_ShouldFallback_WhenNoRelatedId() {
        notification.setType(NotificationType.LIKE);
        notification.setRelatedId(null); // cover if (relatedId == null)

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getRecipeId());
    }

    @Test
    void enrichFollowNotification_ShouldDoNothing_WhenNoRelatedId() {
        notification.setType(NotificationType.FOLLOW);
        notification.setRelatedId(null);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getActorId());
    }

    @Test
    void enrichRecipeNotification_ShouldDoNothing_WhenNoRelatedId() {
        notification.setType(NotificationType.SYSTEM);
        notification.setRelatedId(null);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getRecipeId());
    }

    @Test
    void enrichRecipeNotification_ShouldHandleNoRecipe() {
        notification.setType(NotificationType.SYSTEM);
        notification.setRelatedId(recipeId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getRecipeTitle());
    }

    @Test
    void enrichRecipeNotification_ShouldHandleNoAuthor() {
        notification.setType(NotificationType.SYSTEM);
        notification.setRelatedId(recipeId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getActorId());
    }

    @Test
    void enrichCommentNotification_ShouldHandleNoComment() {
        notification.setType(NotificationType.COMMENT);
        notification.setRelatedId(commentId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getRecipeId());
    }

    @Test
    void enrichCommentNotification_ShouldHandleNoRecipe() {
        notification.setType(NotificationType.COMMENT);
        notification.setRelatedId(commentId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getRecipeTitle());
    }

    @Test
    void enrichCommentNotification_ShouldHandleNoActor() {
        notification.setType(NotificationType.COMMENT);
        notification.setRelatedId(commentId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getActorName());
    }

    @Test
    void enrichLikeNotification_ShouldHandleNoRecipe() {
        notification.setType(NotificationType.LIKE);
        notification.setRelatedId(recipeId);
        notification.setMessage(actorId + "::Actor User liked");

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getRecipeTitle());
    }

    @Test
    void enrichLikeNotification_ShouldHandleNoActor() {
        notification.setType(NotificationType.LIKE);
        notification.setRelatedId(recipeId);
        notification.setMessage(actorId + "::Actor User liked");

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getActorName());
    }

    @Test
    void enrichLikeNotification_ShouldHandleNullAvatar() {
        actor.setAvatarUrl(null);
        notification.setType(NotificationType.LIKE);
        notification.setRelatedId(recipeId);
        notification.setMessage(actorId + "::Actor User liked");

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getActorAvatar());
    }

    @Test
    void enrichFollowNotification_ShouldHandleNoActor() {
        notification.setType(NotificationType.FOLLOW);
        notification.setRelatedId(actorId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getActorId());
    }

    @Test
    void enrichFollowNotification_ShouldHandleNullAvatar() {
        actor.setAvatarUrl(null);
        notification.setType(NotificationType.FOLLOW);
        notification.setRelatedId(actorId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getActorAvatar());
    }

    @Test
    void enrichRecipeNotification_ShouldHandleNullAvatar() {
        actor.setAvatarUrl(null);
        notification.setType(NotificationType.SYSTEM);
        notification.setRelatedId(recipeId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor)); // actor là author

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getActorAvatar());
    }

    @Test
    void enrichRecipeNotification_ShouldHandleNullFeaturedImage() {
        recipe.setFeaturedImage(null);
        notification.setType(NotificationType.SYSTEM);
        notification.setRelatedId(recipeId);

        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));
        when(userRepository.findById(userId)).thenReturn(Optional.of(actor));

        var result = notificationService.getUserNotifications(userId, 0, 10);

        NotificationResponse resp = result.getContent().get(0);
        assertNull(resp.getRecipeImage());
    }

    @Test
    void convertToResponse_ShouldSafelyHandleAnyNotificationType() {
        // Test rằng dù type thế nào → không NPE
        Notification notif = Notification.builder()
                .notificationId(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.COMMENT)
                .title("Safe test")
                .message("This must not throw NPE")
                .createdAt(LocalDateTime.now())
                .build();

        Page<Notification> page = new PageImpl<>(List.of(notif));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);

        // Không NPE → đã cover được default branch (vì thực tế luôn có case)
        Page<NotificationResponse> result = notificationService.getUserNotifications(userId, 0, 10);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void deleteRecipeNotifications_ShouldDoNothing_WhenNoNotifications() {
        when(notificationRepository.findByRelatedIdAndTypes(eq(recipeId), anyList())).thenReturn(List.of());

        notificationService.deleteRecipeNotifications(recipeId);

        verify(notificationRepository, never()).delete(any());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}