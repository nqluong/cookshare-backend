package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    boolean existsByNameIgnoreCase(String name);
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) = LOWER(:name)")
    Optional<Tag> findByNameIgnoreCase(String name);
    Optional<Tag> findBySlug(String slug);

}
