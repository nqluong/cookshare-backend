package com.backend.cookshare.interaction.repository;

import com.backend.cookshare.interaction.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {
    List<SearchHistory> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);
}
