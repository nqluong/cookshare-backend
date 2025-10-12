package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SearchMapper {

    SearchReponse toSearchRecipeResponse(Recipe recipe);
}
