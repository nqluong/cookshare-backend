package com.backend.cookshare.recipe_management.repository;

import com.backend.cookshare.recipe_management.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String name);
    boolean existsBySlug(String slug);
}
