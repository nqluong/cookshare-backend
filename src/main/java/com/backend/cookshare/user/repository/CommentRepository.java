package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // ==================== READ QUERIES ====================

    /**
     * Lấy tất cả comment của một recipe (sắp xếp mới nhất trước)
     */
    Page<Comment> findByRecipeIdOrderByCreatedAtDesc(UUID recipeId, Pageable pageable);

    /**
     * Lấy tất cả comment của một user
     */
    Page<Comment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Lấy comment với parent comment (trả lời)
     */
    List<Comment> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);

    /**
     * Đếm comment của một recipe
     */
    long countByRecipeId(UUID recipeId);

    /**
     * Đếm comment của một user
     */
    long countByUserId(UUID userId);

    /**
     * Tìm comment theo id và recipe id (để verify ownership)
     */
    Optional<Comment> findByCommentIdAndRecipeId(UUID commentId, UUID recipeId);

    /**
     * Tìm comment top level (không có parent)
     */
    List<Comment> findByRecipeIdAndParentCommentIdIsNullOrderByCreatedAtDesc(UUID recipeId);

    /**
     * Lấy comment gần đây nhất của recipe
     */
    Optional<Comment> findFirstByRecipeIdOrderByCreatedAtDesc(UUID recipeId);

    /**
     * Kiểm tra user có bình luận vào recipe này không
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Comment c WHERE c.recipeId = :recipeId AND c.userId = :userId")
    boolean existsByRecipeIdAndUserId(@Param("recipeId") UUID recipeId, @Param("userId") UUID userId);

    /**
     * Lấy tất cả comment của recipe (kể cả reply)
     */
    @Query("SELECT c FROM Comment c WHERE c.recipeId = :recipeId ORDER BY c.parentCommentId ASC, c.createdAt ASC")
    List<Comment> findAllCommentsByRecipeId(@Param("recipeId") UUID recipeId);

    /**
     * Tìm comment chứa mention (@username)
     */
    @Query("SELECT c FROM Comment c WHERE c.recipeId = :recipeId AND c.content LIKE %:mention% ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByMention(@Param("recipeId") UUID recipeId, @Param("mention") String mention);

    // ==================== DELETE QUERIES ====================

    /**
     * Xóa tất cả comment của một recipe (khi recipe bị xóa)
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.recipeId = :recipeId")
    int deleteByRecipeId(@Param("recipeId") UUID recipeId);

    /**
     * Xóa tất cả comment của một user
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);

    /**
     * Xóa tất cả reply của một comment (khi comment bị xóa)
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.parentCommentId = :parentCommentId")
    int deleteByParentCommentId(@Param("parentCommentId") UUID parentCommentId);

    /**
     * Xóa comment cũ hơn N ngày
     */
    @Modifying
    @Query(value = "DELETE FROM comment WHERE created_at < NOW() - INTERVAL :days DAY", nativeQuery = true)
    int deleteOldComments(@Param("days") int days);

}