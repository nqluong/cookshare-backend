package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.RecipeResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RecipeMapper {
    RecipeMapper INSTANCE = Mappers.getMapper(RecipeMapper.class);

    Recipe toEntity(RecipeRequest dto);

    RecipeResponse toResponse(Recipe entity);

    // Cập nhật entity từ DTO mà không đổi recipeId và userId
    default void updateRecipeFromDto(RecipeRequest dto, @MappingTarget Recipe entity) {
        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getSlug() != null) entity.setSlug(dto.getSlug());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getPrepTime() != null) entity.setPrepTime(dto.getPrepTime());
        if (dto.getCookTime() != null) entity.setCookTime(dto.getCookTime());
        if (dto.getServings() != null) entity.setServings(dto.getServings());
        if (dto.getDifficulty() != null) entity.setDifficulty(dto.getDifficulty());
        if (dto.getFeaturedImage() != null) entity.setFeaturedImage(dto.getFeaturedImage());
        if (dto.getInstructions() != null) entity.setInstructions(dto.getInstructions());
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());
        if (dto.getNutritionInfo() != null) entity.setNutritionInfo(dto.getNutritionInfo());
        if (dto.getIsPublished() != null) entity.setIsPublished(dto.getIsPublished());
        if (dto.getIsFeatured() != null) entity.setIsFeatured(dto.getIsFeatured());
        if (dto.getMetaKeywords() != null) entity.setMetaKeywords(dto.getMetaKeywords());
        if (dto.getSeasonalTags() != null) entity.setSeasonalTags(dto.getSeasonalTags());
    }
}
