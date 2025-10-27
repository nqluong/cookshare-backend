package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Tag> findBySlug(String slug);
}
