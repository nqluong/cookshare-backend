package com.backend.cookshare.user.repository;

import com.backend.cookshare.user.entity.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    Page<Collection> findByUserId(UUID userId, Pageable pageable);

    Page<Collection> findByUserIdAndIsPublic(UUID userId, Boolean isPublic, Pageable pageable);

    Optional<Collection> findByCollectionIdAndUserId(UUID collectionId, UUID userId);

    @Query("SELECT COUNT(c) FROM Collection c WHERE c.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(c) FROM Collection c WHERE c.userId = :userId AND c.isPublic = true")
    long countPublicByUserId(@Param("userId") UUID userId);

    boolean existsByNameAndUserId(String name, UUID userId);
}