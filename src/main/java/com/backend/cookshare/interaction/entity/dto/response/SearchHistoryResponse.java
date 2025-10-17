package com.backend.cookshare.interaction.entity.dto.response;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryResponse {
    private UUID searchId;
    private UUID userId;
    private String searchQuery;
    private String searchType;
    private Integer resultCount = 0;
    private LocalDateTime createdAt;
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
