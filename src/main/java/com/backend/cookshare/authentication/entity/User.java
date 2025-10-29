package com.backend.cookshare.authentication.entity;

import com.backend.cookshare.authentication.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    UUID userId;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    String username;

    @Column(name = "email", unique = true, length = 255)
    String email;

    @Column(name = "full_name", length = 255)
    String fullName;

    @Column(name = "password_hash", nullable = false, length = 255)
    String passwordHash;

    @Column(name = "avatar_url", length = 1000)
    String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50)
    @Builder.Default
    UserRole role = UserRole.USER;

    @Column(name = "google_id", unique = true, length = 100)
    String googleId;

    @Column(name = "facebook_id", unique = true, length = 100)
    String facebookId;

    @Column(name = "is_active")
    Boolean isActive = true;

    @Column(name = "email_verified")
    Boolean emailVerified = false;

    @Column(name = "last_active")
    LocalDateTime lastActive;

    @Column(name = "follower_count")
    Integer followerCount = 0;

    @Column(name = "following_count")
    Integer followingCount = 0;

    @Column(name = "recipe_count")
    Integer recipeCount = 0;

    @Column(name = "refresh_token", length = 1000)
    String refreshToken;

    @OneToOne(mappedBy = "user")
    ForgotPassword forgotPassword;

    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    LocalDateTime updatedAt = LocalDateTime.now();

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
