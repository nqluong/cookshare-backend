package com.backend.cookshare.user.controller;

import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.user.dto.FollowRequest;
import com.backend.cookshare.user.dto.FollowResponse;
import com.backend.cookshare.user.dto.UserFollowDto;
import com.backend.cookshare.user.service.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class FollowController {

    private final FollowService followService;

    //Xử lí yêu cầu follow một người khác.
    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<FollowResponse>> followUser(
            @PathVariable UUID userId,
            @RequestBody @Valid FollowRequest request) {

        log.info("POST /api/v1/users/{}/follow - Request: {}", userId, request);

        FollowResponse response = followService.followUser(userId, request.getFollowingId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<FollowResponse>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("Follow thành công")
                        .data(response)
                        .build());
    }

    //Xử lý yêu cầu hủy follow một người dùng.
    @DeleteMapping("/{userId}/follow/{followingId}")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @PathVariable UUID userId,
            @PathVariable UUID followingId) {

        log.info("DELETE /api/v1/users/{}/follow/{}", userId, followingId);

        followService.unfollowUser(userId, followingId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Unfollow thành công")
                .build());
    }

    //Lấy danh sách các follower của người dùng theo phân trang.
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<PageResponse<UserFollowDto>>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/v1/users/{}/followers?page={}&size={}", userId, page, size);

        PageResponse<UserFollowDto> followers = followService.getFollowers(userId, page, size);

        return ResponseEntity.ok(ApiResponse.<PageResponse<UserFollowDto>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách followers thành công")
                .data(followers)
                .build());
    }

    //Lấy danh sách các người dùng mà người dùng đang follow theo phân trang.
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<PageResponse<UserFollowDto>>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/v1/users/{}/following?page={}&size={}", userId, page, size);

        PageResponse<UserFollowDto> following = followService.getFollowing(userId, page, size);

        return ResponseEntity.ok(ApiResponse.<PageResponse<UserFollowDto>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách following thành công")
                .data(following)
                .build());
    }

    //Kiểm tra trạng thái follow giữa hai người dùng.
    @GetMapping("/{userId}/follow/check/{targetUserId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFollowStatus(
            @PathVariable UUID userId,
            @PathVariable UUID targetUserId) {

        log.info("GET /api/v1/users/{}/follow/check/{}", userId, targetUserId);

        boolean isFollowing = followService.isFollowing(userId, targetUserId);

        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .code(HttpStatus.OK.value())
                .message("Kiểm tra trạng thái follow thành công")
                .data(isFollowing)
                .build());
    }

}
