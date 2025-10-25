package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.dto.response.RecipeStepResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import com.backend.cookshare.recipe_management.entity.RecipeStep;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RecipeMapper {
    RecipeMapper INSTANCE = Mappers.getMapper(RecipeMapper.class);

    // Mapping RecipeRequest → Recipe entity
    @Mapping(target = "recipeId", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    Recipe toEntity(RecipeRequest dto);

    // Recipe → Response
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "ingredients", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "categories", ignore = true)
    RecipeResponse toResponse(Recipe entity);

    void updateRecipeFromDto(RecipeRequest dto, @MappingTarget Recipe entity);

    // ---- Mapping Steps & Ingredients ----
    RecipeStep toStepEntity(RecipeStepRequest dto);
    RecipeStepResponse toStepResponse(RecipeStep entity);

    RecipeIngredientResponse toIngredientResponse(RecipeIngredient entity);
    List<RecipeStepResponse> toStepResponseList(List<RecipeStep> entities);
    List<RecipeIngredientResponse> toIngredientResponseList(List<RecipeIngredient> entities);
}
