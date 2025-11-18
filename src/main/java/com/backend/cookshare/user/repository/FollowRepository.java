package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.entity.Follow;
import com.backend.cookshare.user.entity.FollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow,FollowId> {

    //Kiểm tra xem quan hệ follow giữa hai người dùng đã tồn tại chưa.
    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    //Tìm quan hệ follow giữa hai người dùng.
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    //Đếm số lượng follower của một người dùng.
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followingId = :userId")
    long countFollowers(@Param("userId") UUID userId);

    //Đếm số lượng người mà một người dùng đang follow.
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followerId = :userId")
    long countFollowing(@Param("userId") UUID userId);

    //Lấy danh sách ID của các người dùng mà một người dùng đang follow theo phân trang.
    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :userId")
    Page<UUID> findFollowingIds(@Param("userId") UUID userId, Pageable pageable);
    @Query("SELECT f.followingId FROM Follow f WHERE f.followerId = :userId")
    List<UUID> findAllFollowingIdsByUser(UUID userId);
    //Lấy danh sách ID của các follower của một người dùng theo phân trang.
    @Query("SELECT f.followerId FROM Follow f WHERE f.followingId = :userId")
    Page<UUID> findFollowerIds(@Param("userId") UUID userId, Pageable pageable);

    // Lấy tất cả follower IDs (không phân trang) - DÙNG ĐỂ GỬI NOTIFICATION
    @Query("SELECT f.followerId FROM Follow f WHERE f.followingId = :userId")
    List<UUID> findAllFollowerIdsByUser(@Param("userId") UUID userId);
}
