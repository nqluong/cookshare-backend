//package com.backend.cookshare.interaction.entity;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.GenericGenerator;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Entity
//@Table(name = "recipe_comments")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class RecipeComment {
//
//    @Id
//    @GeneratedValue(generator = "uuid2")
//    @GenericGenerator(name = "uuid2", strategy = "uuid2")
//    @Column(name = "comment_id", columnDefinition = "uuid")
//    private UUID commentId;
//
//    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
//    private UUID userId;
//
//    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
//    private UUID recipeId;
//
//    @Column(name = "parent_id", columnDefinition = "uuid")
//    private UUID parentId;
//
//    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
//    private String content;
//
//    @Column(name = "like_count")
//    @Builder.Default
//    private Integer likeCount = 0;
//
//    @Column(name = "is_edited")
//    @Builder.Default
//    private Boolean isEdited = false;
//
//    @Column(name = "is_deleted")
//    @Builder.Default
//    private Boolean isDeleted = false;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }
//}
