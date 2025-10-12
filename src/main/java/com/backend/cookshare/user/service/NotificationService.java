package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.user.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.user.entity.Notification;
import com.backend.cookshare.user.enums.NotificationType;
import com.backend.cookshare.user.enums.RelatedType;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    //Tạo thông báo follow cho người dùng được follow.
    @Transactional
    public void createFollowNotification(UUID followerId, UUID followingId) {
        log.info("Creating follow notification from {} to {}", followerId, followingId);

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .userId(followingId)
                .type(NotificationType.FOLLOW)
                .title("Người theo dõi mới")
                .message(follower.getUsername() + " đã bắt đầu theo dõi bạn")
                .relatedId(followerId)
                .relatedType(RelatedType.user)
                .isRead(false)
                .isSent(true)
                .build();

        notificationRepository.save(notification);

        log.info("Follow notification created successfully");
    }
}
