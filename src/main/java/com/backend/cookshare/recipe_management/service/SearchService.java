package com.backend.cookshare.recipe_management.service;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import org.springframework.data.domain.Pageable;


import java.util.List;
public interface SearchService {

    PageResponse<SearchReponse> searchRecipesByName(String title ,Pageable pageable) ;
    PageResponse<SearchReponse>searchRecipesByIngredient(String ingredient,Pageable pageable);
}
