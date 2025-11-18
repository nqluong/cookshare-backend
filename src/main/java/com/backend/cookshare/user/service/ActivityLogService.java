package com.backend.cookshare.user.service;

import com.backend.cookshare.user.entity.ActivityLog;
import com.backend.cookshare.user.enums.ActivityType;
import com.backend.cookshare.user.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Log hoạt động comment
     */
    @Transactional
    public void logCommentActivity(UUID userId, UUID commentId, UUID recipeId, String action) {
        ActivityType activityType = switch (action) {
            case "CREATE" -> ActivityType.CREATE;
            case "UPDATE" -> ActivityType.UPDATE;
            case "DELETE" -> ActivityType.DELETE;
            default -> ActivityType.VIEW;
        };

        logActivity(userId, activityType, commentId);
    }

    /**
     * Log hoạt động like
     */
    @Transactional
    public void logLikeActivity(UUID userId, UUID recipeId, String action) {
        ActivityType activityType = action.equals("CREATE") ? ActivityType.CREATE : ActivityType.DELETE;
        logActivity(userId, activityType, recipeId);
    }

    /**
     * Log hoạt động follow
     */
    @Transactional
    public void logFollowActivity(UUID userId, UUID targetUserId, String action) {
        ActivityType activityType = action.equals("CREATE") ? ActivityType.CREATE : ActivityType.DELETE;
        logActivity(userId, activityType, targetUserId);
    }

    /**
     * Log hoạt động xem recipe
     */
    @Transactional
    public void logViewActivity(UUID userId, UUID recipeId) {
        logActivity(userId, ActivityType.VIEW, recipeId);
    }

    /**
     * Log hoạt động tìm kiếm
     */
    @Transactional
    public void logSearchActivity(UUID userId, String searchQuery) {
        logActivity(userId, ActivityType.SEARCH, null);
    }

    /**
     * Log hoạt động login/logout
     */
    @Transactional
    public void logAuthActivity(UUID userId, ActivityType activityType) {
        if (activityType == ActivityType.LOGIN || activityType == ActivityType.LOGOUT) {
            logActivity(userId, activityType, null);
        }
    }

    /**
     * Log hoạt động share
     */
    @Transactional
    public void logShareActivity(UUID userId, UUID recipeId) {
        logActivity(userId, ActivityType.SHARE, recipeId);
    }

    /**
     * Log hoạt động download
     */
    @Transactional
    public void logDownloadActivity(UUID userId, UUID recipeId) {
        logActivity(userId, ActivityType.DOWNLOAD, recipeId);
    }

    /**
     * Log hoạt động print
     */
    @Transactional
    public void logPrintActivity(UUID userId, UUID recipeId) {
        logActivity(userId, ActivityType.PRINT, recipeId);
    }

    /**
     * Log hoạt động collection (tạo, xóa, thêm recipe, xóa recipe)
     */
    @Transactional
    public void logCollectionActivity(UUID userId, UUID collectionId, String action) {
        ActivityType activityType = switch (action) {
            case "CREATE" -> ActivityType.CREATE;
            case "UPDATE" -> ActivityType.UPDATE;
            case "DELETE" -> ActivityType.DELETE;
            case "ADD_RECIPE" -> ActivityType.CREATE; // Thêm recipe vào collection
            case "REMOVE_RECIPE" -> ActivityType.DELETE; // Xóa recipe khỏi collection
            default -> ActivityType.VIEW;
        };

        logActivity(userId, activityType, collectionId);
    }

    /**
     * Log hoạt động recipe (tạo, sửa, xóa, duyệt)
     */
    @Transactional
    public void logRecipeActivity(UUID userId, UUID recipeId, String action) {
        ActivityType activityType = switch (action) {
            case "CREATE" -> ActivityType.CREATE;
            case "UPDATE" -> ActivityType.UPDATE;
            case "DELETE" -> ActivityType.DELETE;
            case "APPROVE" -> ActivityType.UPDATE; // Admin duyệt
            case "REJECT" -> ActivityType.UPDATE;  // Admin từ chối
            default -> ActivityType.VIEW;
        };

        logActivity(userId, activityType, recipeId);
    }

    /**
     * Core method để log activity với thông tin request
     */
    private void logActivity(UUID userId, ActivityType activityType, UUID targetId) {
        try {
            // Lấy thông tin request hiện tại
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            String ipAddress = null;
            String userAgent = null;
            String referrer = null;

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = getClientIpAddress(request);
                userAgent = request.getHeader("User-Agent");
                referrer = request.getHeader("Referer");
            }

            ActivityLog log = ActivityLog.builder()
                    .userId(userId)
                    .activityType(activityType)
                    .targetId(targetId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .build();

            activityLogRepository.save(log);
        } catch (Exception e) {
            // Log error nhưng không throw exception để không ảnh hưởng business logic
            System.err.println("Error logging activity: " + e.getMessage());
        }
    }

    /**
     * Lấy IP address thực của client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Trường hợp có nhiều IP (proxy chain), lấy IP đầu tiên
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}