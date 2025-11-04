package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // Lấy tất cả comment gốc của recipe (không có parentCommentId)
    @Query("SELECT c FROM Comment c WHERE c.recipeId = :recipeId AND c.parentCommentId IS NULL ORDER BY c.createdAt DESC")
    Page<Comment> findRootCommentsByRecipeId(@Param("recipeId") UUID recipeId, Pageable pageable);

    // Lấy tất cả reply của một comment
    @Query("SELECT c FROM Comment c WHERE c.parentCommentId = :parentCommentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentCommentId(@Param("parentCommentId") UUID parentCommentId);

    // Đếm số lượng reply
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parentCommentId = :parentCommentId")
    Integer countRepliesByParentCommentId(@Param("parentCommentId") UUID parentCommentId);

    // Lấy comment theo ID với user info
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.commentId = :commentId")
    Comment findByIdWithUser(@Param("commentId") UUID commentId);
}