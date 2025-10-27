package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.interaction.entity.dto.response.SearchHistoryResponse;
import com.backend.cookshare.recipe_management.dto.response.IngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import org.springframework.data.domain.Pageable;


import java.util.List;
public interface SearchService {

    PageResponse<SearchReponse> searchRecipesByName(String title ,Pageable pageable) ;
    PageResponse<SearchReponse>searchRecipesByIngredient(String title,List<String> ingredients,Pageable pageable);
    List<IngredientResponse>  top10MostUsedIngredients();
    List<SearchHistoryResponse> getSearchHistory();
    PageResponse<SearchReponse> searchRecipesByfullName(String keyword, Pageable pageable);
}
