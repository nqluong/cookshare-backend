package com.backend.cookshare.authentication.repository;

import com.backend.cookshare.authentication.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    //Tìm người dùng theo username.
    Optional<User> findByUsername(String username);
    User findByUserId(UUID userId);
    //Tìm người dùng theo email.
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByFacebookId(String facebookId);

    @Query("SELECT u FROM User u WHERE u.username = ?1 OR u.email = ?1")
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    //Kiểm tra xem có tồn tại người dùng với username đã cho không.
    boolean existsByUsername(String username);

    //Kiểm tra xem có tồn tại người dùng với email đã cho không.
    boolean existsByEmail(String email);

    User findByRefreshTokenAndUsername(String token, String username);

    // Admin methods
    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findAllWithSearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = :isActive")
    long countByIsActive(@Param("isActive") Boolean isActive);

    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> findByFullNameContainingIgnoreCase(@Param("query") String query);
    Page<User>findByFullNameContainingIgnoreCase(@Param("query") String query,Pageable pageable);

    /**
     * Tăng recipe_count của user
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET recipe_count = COALESCE(recipe_count, 0) + 1 WHERE user_id = :userId", nativeQuery = true)
    void incrementRecipeCount(@Param("userId") UUID userId);

    /**
     * Giảm recipe_count của user (không cho phép âm)
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE users SET recipe_count = GREATEST(COALESCE(recipe_count, 0) - 1, 0) WHERE user_id = :userId", nativeQuery = true)
    void decrementRecipeCount(@Param("userId") UUID userId);
}