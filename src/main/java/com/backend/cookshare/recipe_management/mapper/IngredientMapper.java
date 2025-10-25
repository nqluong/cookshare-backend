package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.*;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface IngredientMapper {

    Ingredient toEntity(IngredientRequest request);

    RecipeIngredientResponse toResponse(Ingredient ingredient);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateIngredientFromDto(IngredientRequest request, @MappingTarget Ingredient ingredient);
}
