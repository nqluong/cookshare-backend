package com.backend.cookshare.user.controller;

import com.backend.cookshare.user.dto.NotificationDto;
import com.backend.cookshare.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private final NotificationService notificationService;

     //Lấy danh sách thông báo của user (có phân trang)
    @GetMapping
    public ResponseEntity<?> getUserNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        try {
            String userId = authentication.getName();

            // Validate phân trang
            if (page < 0 || size <= 0 || size > 100) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Tham số phân trang không hợp lệ (page >= 0, 0 < size <= 100)"));
            }

            UUID userUUID = UUID.fromString(userId);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
            Page<NotificationDto> notifications = notificationService.getUserNotifications(userUUID, pageable);

            log.info("👤 User {} retrieved {} notifications (page {}/{})",
                    userId, notifications.getSize(), page, notifications.getTotalPages());

            return ResponseEntity.ok(createPageResponse(notifications));
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid UUID format: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("UUID không hợp lệ"));
        } catch (Exception e) {
            log.error("❌ Error retrieving notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi server: " + e.getMessage()));
        }
    }

    /**
     * Lấy số lượng thông báo chưa đọc
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        try {
            String userId = authentication.getName();
            UUID userUUID = UUID.fromString(userId);

            long unreadCount = notificationService.getUnreadCount(userUUID);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Lấy số thông báo chưa đọc thành công");
            response.put("unreadCount", unreadCount);

            log.info("👤 User {} has {} unread notifications", userId, unreadCount);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid UUID format");
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("UUID không hợp lệ"));
        } catch (Exception e) {
            log.error("❌ Error getting unread count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi lấy số thông báo chưa đọc"));
        }
    }

    /**
     * GET /api/v1/notifications/{notificationId}
     * Lấy chi tiết một thông báo
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<?> getNotificationDetail(
            Authentication authentication,
            @PathVariable UUID notificationId) {
        try {
            String userId = authentication.getName();
            UUID userUUID = UUID.fromString(userId);

            NotificationDto notification = notificationService.getNotificationDetail(notificationId, userUUID);

            log.info("👤 User {} retrieved notification detail: {}", userId, notificationId);
            return ResponseEntity.ok(notification);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid UUID format");
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("UUID không hợp lệ"));
        } catch (Exception e) {
            log.error("❌ Error getting notification detail: {}", e.getMessage());
            if (e.getMessage().contains("Không tìm thấy")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Thông báo không tồn tại"));
            } else if (e.getMessage().contains("Không có quyền")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Không có quyền truy cập thông báo này"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi server"));
        }
    }

    // ==================== UPDATE ENDPOINTS ====================

    /**
     * POST /api/v1/notifications/read/{notificationId}
     * Đánh dấu một thông báo là đã đọc
     */
    @PostMapping("/read/{notificationId}")
    public ResponseEntity<?> markAsRead(
            Authentication authentication,
            @PathVariable UUID notificationId) {
        try {
            String userId = authentication.getName();
            UUID userUUID = UUID.fromString(userId);

            NotificationDto updated = notificationService.markAsRead(notificationId, userUUID);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Thông báo đã được đánh dấu là đã đọc");
            response.put("data", updated);

            log.info("✅ User {} marked notification {} as read", userId, notificationId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid UUID format");
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("UUID không hợp lệ"));
        } catch (Exception e) {
            log.error("❌ Error marking notification as read: {}", e.getMessage());
            if (e.getMessage().contains("Không tìm thấy")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Thông báo không tồn tại"));
            } else if (e.getMessage().contains("Không có quyền")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Không có quyền cập nhật thông báo này"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi server"));
        }
    }

    /**
     * PUT /api/v1/notifications/read-all
     * Đánh dấu tất cả thông báo là đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        try {
            String userId = authentication.getName();
            UUID userUUID = UUID.fromString(userId);

            int updatedCount = notificationService.markAllAsRead(userUUID);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Tất cả thông báo đã được đánh dấu là đã đọc");
            response.put("updatedCount", updatedCount);

            log.info("✅ User {} marked all {} notifications as read", userId, updatedCount);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid UUID format");
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("UUID không hợp lệ"));
        } catch (Exception e) {
            log.error("❌ Error marking all notifications as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi đánh dấu tất cả thông báo"));
        }
    }

    // ==================== DELETE ENDPOINTS ====================

    /**
     * DELETE /api/v1/notifications/{notificationId}
     * Xóa một thông báo
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(
            Authentication authentication,
            @PathVariable UUID notificationId) {
        try {
            String userId = authentication.getName();
            UUID userUUID = UUID.fromString(userId);

            notificationService.deleteNotification(notificationId, userUUID);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Thông báo đã được xóa");

            log.info("✅ User {} deleted notification: {}", userId, notificationId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid UUID format");
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("UUID không hợp lệ"));
        } catch (Exception e) {
            log.error("❌ Error deleting notification: {}", e.getMessage());
            if (e.getMessage().contains("Không tìm thấy")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Thông báo không tồn tại"));
            } else if (e.getMessage().contains("Không có quyền")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Không có quyền xóa thông báo này"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi server"));
        }
    }

    /**
     * DELETE /api/v1/notifications/all
     * Xóa tất cả thông báo của user
     *
     * ⚠️ LƯU Ý: Endpoint này phải được đặt DƯỚI endpoint /{notificationId}
     * để tránh xung đột route pattern
     */
    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllNotifications(Authentication authentication) {
        try {
            String userId = authentication.getName();
            UUID userUUID = UUID.fromString(userId);

            int deletedCount = notificationService.deleteAllNotifications(userUUID);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Tất cả thông báo đã được xóa");
            response.put("deletedCount", deletedCount);

            log.info("✅ User {} deleted all {} notifications", userId, deletedCount);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid UUID format");
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("UUID không hợp lệ"));
        } catch (Exception e) {
            log.error("❌ Error deleting all notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Lỗi khi xóa tất cả thông báo"));
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Tạo response cho lỗi
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        return response;
    }

    /**
     * Tạo response cho phân trang
     */
    private Map<String, Object> createPageResponse(Page<NotificationDto> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Lấy danh sách thông báo thành công");
        response.put("data", page.getContent());
        response.put("pagination", Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "hasNext", page.hasNext(),
                "hasPrevious", page.hasPrevious(),
                "isFirst", page.isFirst(),
                "isLast", page.isLast()
        ));
        return response;
    }
}