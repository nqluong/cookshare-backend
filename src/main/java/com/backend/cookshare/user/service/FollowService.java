package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.user.dto.FollowResponse;
import com.backend.cookshare.user.dto.UserFollowDto;
import com.backend.cookshare.user.entity.Follow;
import com.backend.cookshare.user.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final NotificationService notificationService;
    private final PageMapper pageMapper;

    //Xử lý hành động follow một người dùng khác.
    @Transactional
    public FollowResponse followUser(UUID followerId, UUID followingId) {
        log.info("User {} attempting to follow user {}", followerId, followingId);

        // Kiểm tra không thể follow chính mình
        if (followerId.equals(followingId)) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_YOURSELF);
        }

        // Kiểm tra người follow có tồn tại và active
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!follower.getIsActive()) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }

        // Kiểm tra người được follow có tồn tại và active
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!following.getIsActive()) {
            throw new CustomException(ErrorCode.USER_NOT_ACTIVE);
        }

        // Kiểm tra đã follow chưa
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new CustomException(ErrorCode.ALREADY_FOLLOWING);
        }

        // Tạo quan hệ follow
        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .build();

        followRepository.save(follow);

        // Cập nhật follower count
        following.setFollowerCount(following.getFollowerCount() + 1);
        follower.setFollowingCount(follower.getFollowingCount() + 1);

        userRepository.save(following);
        userRepository.save(follower);

        // ========== 🔔 TẠO FOLLOW NOTIFICATION ==========
        notificationService.createFollowNotification(followerId, followingId);

        // Gửi realtime notification via WebSocket
        //webSocketNotificationSender.sendFollowNotification(followerId, followingId, follower.getUsername());

        log.info("User {} successfully followed user {}", followerId, followingId);

        return FollowResponse.builder()
                .followerId(followerId)
                .followingId(followingId)
                .followerUsername(follower.getUsername())
                .followingUsername(following.getUsername())
                .createdAt(follow.getCreatedAt())
                .message("Đã follow thành công")
                .build();
    }

    //Xử lý hành động hủy follow một người dùng.
    @Transactional
    public void unfollowUser(UUID followerId, UUID followingId) {
        log.info("User {} attempting to unfollow user {}", followerId, followingId);

        // Kiểm tra quan hệ follow tồn tại
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOLLOWING));

        // Xóa quan hệ follow
        followRepository.delete(follow);

        // Cập nhật follower count
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        following.setFollowerCount(Math.max(0, following.getFollowerCount() - 1));
        follower.setFollowingCount(Math.max(0, follower.getFollowingCount() - 1));

        userRepository.save(following);
        userRepository.save(follower);

        log.info("User {} successfully unfollowed user {}", followerId, followingId);
    }

    //Lấy danh sách follower của một người dùng theo phân trang.
    @Transactional(readOnly = true)
    public PageResponse<UserFollowDto> getFollowers(UUID userId, int page, int size) {
        log.info("Getting followers for user {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> followerIds = followRepository.findFollowerIds(userId, pageable);

        List<UserFollowDto> followers = followerIds.getContent().stream()
                .map(followerId -> {
                    User user = userRepository.findById(followerId).orElse(null);
                    if (user != null) {
                        return mapToUserFollowDto(user, userId);
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(followers, followerIds);
    }

    //Lấy danh sách người dùng mà một người dùng đang follow theo phân trang.
    @Transactional(readOnly = true)
    public PageResponse<UserFollowDto> getFollowing(UUID userId, int page, int size) {
        log.info("Getting following for user {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<UUID> followingIds = followRepository.findFollowingIds(userId, pageable);

        List<UserFollowDto> following = followingIds.getContent().stream()
                .map(followingId -> {
                    User user = userRepository.findById(followingId).orElse(null);
                    if (user != null) {
                        return mapToUserFollowDto(user, userId);
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(following, followingIds);
    }

    //Kiểm tra trạng thái follow giữa hai người dùng.
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID followerId, UUID followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    //Chuyển đổi đối tượng User thành UserFollowDto.
    private UserFollowDto mapToUserFollowDto(User user, UUID currentUserId) {
        return UserFollowDto.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .followerCount(user.getFollowerCount())
                .followingCount(user.getFollowingCount())
                .isFollowing(currentUserId != null ?
                        followRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getUserId()) : null)
                .build();
    }

}
