package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.ApiResponse;
import com.backend.cookshare.recipe_management.dto.response.IngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.mapper.SearchMapper;
import com.backend.cookshare.recipe_management.repository.IngredientRepository;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.recipe_management.service.SearchService;
import com.backend.cookshare.recipe_management.specification.RecipeSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SearchServiceImpl implements SearchService {
    RecipeRepository recipeRepository;
    SearchMapper searchMapper;
    IngredientRepository ingredientRepository;
    @Override
    public PageResponse<SearchReponse> searchRecipesByName(String keyword, Pageable pageable) {
        if (keyword== null || keyword.trim().isEmpty()) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_EMPTY,"");
        }
        if(keyword.trim().length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_SHORT,"");
        }
        if(keyword.trim().length() > 80) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_LONG,"");
        }
        if (!keyword.trim().matches("^[\\p{L}\\p{N}\\s\\-']+$")) {
            throw new CustomException(ErrorCode.INVALID_CHARACTERS,"");
        }

        Specification<Recipe> spec = RecipeSpecification.hasNameLike(keyword);
        Page<Recipe> recipePage = recipeRepository.findAll(spec, pageable);

        List<SearchReponse> content = recipePage.getContent().stream()
                .map(searchMapper::toSearchRecipeResponse)
                .toList();

        return buildPageResponse(recipePage, content);
    }
   @Override
    public PageResponse<SearchReponse> searchRecipesByIngredient(String title, List<String> ingredientNames, Pageable pageable) {
       if (title== null || title.trim().isEmpty()) {
           throw new CustomException(ErrorCode.SEARCH_QUERY_EMPTY,"");
       }
       if(title.trim().length() < 2) {
           throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_SHORT,"");
       }
       if(title.trim().length() > 80) {
           throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_LONG,"");
       }
       if (!title.trim().matches("^[\\p{L}\\p{N}\\s\\-']+$")) {
           throw new CustomException(ErrorCode.INVALID_CHARACTERS,"");
       }
       Specification<Recipe> spec = RecipeSpecification.hasRecipesByIngredients(title, ingredientNames);
       Page<Recipe> recipePage = recipeRepository.findAll(spec, pageable);

       List<SearchReponse> content = recipePage.getContent().stream()
               .map(searchMapper::toSearchRecipeResponse)
               .toList();

       return buildPageResponse(recipePage, content);
    }
    private <T> PageResponse<T> buildPageResponse(Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .numberOfElements(page.getNumberOfElements())
                .sorted(page.getSort().isSorted())
                .build();
    }
    @Override
    public List<IngredientResponse> top10MostUsedIngredients() {
        List<IngredientResponse> ingredients = ingredientRepository.findTop10MostUsedIngredients()
                .stream()
                .map(searchMapper::toIngredientResponseFromArray)
                .toList();
        return ingredients;
    }

}
