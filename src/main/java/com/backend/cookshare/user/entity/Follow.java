package com.backend.cookshare.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "follows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(FollowId.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Follow {

    @Id
    @Column(name = "follower_id", columnDefinition = "uuid")
    UUID followerId;

    @Id
    @Column(name = "following_id", columnDefinition = "uuid")
    UUID followingId;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
