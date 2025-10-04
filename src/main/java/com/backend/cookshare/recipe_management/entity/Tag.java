package com.backend.cookshare.recipe_management.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tag_id", columnDefinition = "uuid")
    private UUID tagId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", unique = true, length = 100)
    private String slug;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "is_trending")
    @Builder.Default
    private Boolean isTrending = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
