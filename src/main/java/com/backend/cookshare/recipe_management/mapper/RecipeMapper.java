package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.entity.*;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RecipeMapper {

    RecipeMapper INSTANCE = Mappers.getMapper(RecipeMapper.class);

    // ============================================
    // 🔹 ENTITY <-> DTO CHÍNH
    // ============================================
    Recipe toEntity(RecipeRequest dto);
    RecipeResponse toResponse(Recipe entity);
    List<RecipeResponse> toResponseList(List<Recipe> entities);

    // ============================================
    // 🔹 CẬP NHẬT ENTITY TỪ DTO
    // ============================================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    default void updateRecipeFromDto(RecipeRequest dto, @MappingTarget Recipe entity) {
        if (dto == null) return;

        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getPrepTime() != null) entity.setPrepTime(dto.getPrepTime());
        if (dto.getCookTime() != null) entity.setCookTime(dto.getCookTime());
        if (dto.getServings() != null) entity.setServings(dto.getServings());
        if (dto.getDifficulty() != null) entity.setDifficulty(dto.getDifficulty());
        if (dto.getFeaturedImage() != null) entity.setFeaturedImage(dto.getFeaturedImage());
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());
        if (dto.getNutritionInfo() != null) entity.setNutritionInfo(dto.getNutritionInfo());
        if (dto.getIsPublished() != null) entity.setIsPublished(dto.getIsPublished());
        if (dto.getIsFeatured() != null) entity.setIsFeatured(dto.getIsFeatured());
        if (dto.getMetaKeywords() != null) entity.setMetaKeywords(dto.getMetaKeywords());
        if (dto.getSeasonalTags() != null) entity.setSeasonalTags(dto.getSeasonalTags());
    }

    // ============================================
    // 🔹 MAPPING CHO STEP
    // ============================================
    default RecipeStep toStepEntity(RecipeStepRequest dto, Recipe recipe) {
        if (dto == null) return null;
        return RecipeStep.builder()
                .recipe(recipe)
                .stepNumber(dto.getStepNumber())
                .instruction(dto.getInstruction())
                .imageUrl(dto.getImageUrl())
                .videoUrl(dto.getVideoUrl())
                .estimatedTime(dto.getEstimatedTime())
                .tips(dto.getTips())
                .build();
    }

    default List<RecipeStep> toStepEntities(List<RecipeStepRequest> dtos, Recipe recipe) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream()
                .map(dto -> toStepEntity(dto, recipe))
                .collect(Collectors.toList());
    }

    // ============================================
    // 🔹 MAPPING CHO INGREDIENT
    // ============================================
    default RecipeResponse.RecipeIngredientResponse toIngredientResponse(RecipeIngredient entity) {
        if (entity == null) return null;
        Ingredient ingredient = entity.getIngredient();

        return RecipeResponse.RecipeIngredientResponse.builder()
                .ingredientId(ingredient != null ? ingredient.getIngredientId() : null)
                .name(ingredient != null ? ingredient.getName() : null)
                .description(ingredient != null ? ingredient.getDescription() : null)
                .quantity(entity.getQuantity())
                .unit(entity.getUnit())
                .notes(entity.getNotes())
                .orderIndex(entity.getOrderIndex())
                .build();
    }

    default List<RecipeResponse.RecipeIngredientResponse> toIngredientResponses(List<RecipeIngredient> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(this::toIngredientResponse)
                .collect(Collectors.toList());
    }


    // ============================================
    // 🔹 MAPPING CHO TAG
    // ============================================
    default List<RecipeTag> toTagEntities(List<UUID> tagIds, UUID recipeId) {
        if (tagIds == null) return Collections.emptyList();
        return tagIds.stream()
                .map(tagId -> RecipeTag.builder()
                        .recipeId(recipeId)
                        .tagId(tagId)
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================
    // 🔹 MAPPING CHO CATEGORY
    // ============================================
    default List<RecipeCategory> toCategoryEntities(List<UUID> categoryIds, UUID recipeId) {
        if (categoryIds == null) return Collections.emptyList();
        return categoryIds.stream()
                .map(categoryId -> RecipeCategory.builder()
                        .recipeId(recipeId)
                        .categoryId(categoryId)
                        .build())
                .collect(Collectors.toList());
    }

    // ============================================
    // 🔹 RESPONSE CHO TAG & CATEGORY
    // ============================================
    default RecipeResponse.TagResponse toTagResponse(Tag tag) {
        if (tag == null) return null;
        return RecipeResponse.TagResponse.builder()
                .tagId(tag.getTagId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .color(tag.getColor())
                .usageCount(tag.getUsageCount())
                .isTrending(tag.getIsTrending())
                .createdAt(tag.getCreatedAt())
                .build();
    }

    default RecipeResponse.CategoryResponse toCategoryResponse(Category category) {
        if (category == null) return null;
        return RecipeResponse.CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .parentId(category.getParentId())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .build();
    }

    default List<RecipeResponse.TagResponse> toTagResponses(List<Tag> tags) {
        if (tags == null) return Collections.emptyList();
        return tags.stream().map(this::toTagResponse).collect(Collectors.toList());
    }

    default List<RecipeResponse.CategoryResponse> toCategoryResponses(List<Category> categories) {
        if (categories == null) return Collections.emptyList();
        return categories.stream().map(this::toCategoryResponse).collect(Collectors.toList());
    }
}
