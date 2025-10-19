package com.backend.cookshare.user.entity;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.recipe_management.entity.Recipe;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_id", columnDefinition = "uuid")
    private UUID commentId;

    // 🔗 Liên kết đến Recipe
    @Column(name = "recipe_id", nullable = false, columnDefinition = "uuid")
    private UUID recipeId;

    // 🔗 Liên kết đến User
    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    // 💬 Nội dung bình luận
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // ✅ Nếu là bình luận trả lời -> lưu ID của bình luận cha
    @Column(name = "parent_comment_id", columnDefinition = "uuid")
    private UUID parentCommentId;

    // ⏰ Thời gian tạo & cập nhật
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 👇 Liên kết JPA đến các bảng khác
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", insertable = false, updatable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @OneToMany
    @JoinColumn(name = "parent_comment_id")
    private List<Comment> replies;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

