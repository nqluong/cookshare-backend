package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RecipeMapper {

    RecipeMapper INSTANCE = Mappers.getMapper(RecipeMapper.class);

    Recipe toEntity(RecipeRequest dto);

    RecipeResponse toResponse(Recipe entity);

    // ✅ Thêm để map danh sách công thức → danh sách response
    List<RecipeResponse> toResponseList(List<Recipe> entities);

    default void updateRecipeFromDto(RecipeRequest dto, @MappingTarget Recipe entity) {
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

    // ✅ Convert Step DTO → Entity
    default RecipeStep toStepEntity(RecipeStepRequest dto, UUID recipeId) {
        return RecipeStep.builder()
                .recipeId(recipeId)
                .stepNumber(dto.getStepNumber())
                .instruction(dto.getInstruction())
                .imageUrl(dto.getImageUrl())
                .videoUrl(dto.getVideoUrl())
                .estimatedTime(dto.getEstimatedTime())
                .tips(dto.getTips())
                .build();
    }

    // ✅ Convert Ingredient DTO → Entity
    default RecipeIngredient toIngredientEntity(RecipeIngredientRequest dto, UUID recipeId) {
        return RecipeIngredient.builder()
                .recipeId(recipeId)
                .ingredientId(dto.getIngredientId())
                .quantity(dto.getQuantity())
                .unit(dto.getUnit())
                .notes(dto.getNotes())
                .orderIndex(dto.getOrderIndex())
                .build();
    }

    // ✅ Convert List Step DTO → Entity
    default List<RecipeStep> toStepEntities(List<RecipeStepRequest> list, UUID recipeId) {
        return list.stream()
                .map(s -> toStepEntity(s, recipeId))
                .collect(Collectors.toList());
    }

    // ✅ Convert List Ingredient DTO → Entity
    default List<RecipeIngredient> toIngredientEntities(List<RecipeIngredientRequest> list, UUID recipeId) {
        return list.stream()
                .map(i -> toIngredientEntity(i, recipeId))
                .collect(Collectors.toList());
    }
}
