package com.backend.cookshare.recipe_management.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "recipe_tags")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RecipeTagId.class)
public class RecipeTag {

    @Id
    @Column(name = "recipe_id", columnDefinition = "uuid")
    private UUID recipeId;

    @Id
    @Column(name = "tag_id", columnDefinition = "uuid")
    private UUID tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", insertable = false, updatable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private Tag tag;
}
