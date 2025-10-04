package com.backend.cookshare.interaction.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "search_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "search_id", columnDefinition = "uuid")
    private UUID searchId;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "search_query", nullable = false, columnDefinition = "TEXT")
    private String searchQuery;

    @Column(name = "search_type", length = 50)
    private String searchType;

    @Column(name = "result_count")
    @Builder.Default
    private Integer resultCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
