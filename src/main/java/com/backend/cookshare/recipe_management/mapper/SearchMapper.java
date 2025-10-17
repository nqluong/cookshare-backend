package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.response.IngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.entity.Recipe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SearchMapper {
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    SearchReponse toSearchRecipeResponse(Recipe recipe);
    IngredientResponse toIngredientResponse(Ingredient ingredient);
    default IngredientResponse toIngredientResponseFromArray(Object[] row) {
        if (row == null || row.length < 3) {
            return null;
        }
        return IngredientResponse.builder()
                .ingredientId(UUID.fromString(row[0].toString()))
                .name((String) row[1])
                .recipeCount(((Number) row[2]).intValue())
                .build();
    }
}
