package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.interaction.entity.SearchHistory;
import com.backend.cookshare.interaction.dto.response.SearchHistoryResponse;
import com.backend.cookshare.interaction.mapper.SearchHistoryMapper;
import com.backend.cookshare.interaction.repository.SearchHistoryRepository;
import com.backend.cookshare.recipe_management.dto.response.IngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SearchServiceImpl implements SearchService {
    RecipeRepository recipeRepository;
    SearchMapper searchMapper;
    IngredientRepository ingredientRepository;
    SearchHistoryRepository searchHistoryRepository;
    UserRepository userRepository;
    SearchHistoryMapper searchHistoryMapper;
    FirebaseStorageService firebaseStorageService;
    @Override
    public PageResponse<SearchReponse> searchRecipesByName(String keyword, Pageable pageable) {
        if (keyword== null || keyword.trim().isEmpty()) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_EMPTY);
        }
        if(keyword.trim().length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_SHORT);
        }
        if(keyword.trim().length() > 80) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_LONG);
        }
        if (!keyword.trim().matches("^[\\p{L}\\p{N}\\s\\-']+$")) {
            throw new CustomException(ErrorCode.INVALID_CHARACTERS);
        }

        Specification<Recipe> spec = RecipeSpecification.hasNameLike(keyword);
        Page<Recipe> recipePage = recipeRepository.findAll(spec, pageable);

        List<SearchReponse> content = recipePage.getContent().stream()
                .map(searchMapper::toSearchRecipeResponse)
                .peek(r -> {
                    r.setFeaturedImage(firebaseStorageService.convertPathToFirebaseUrl(r.getFeaturedImage()));
                })
                .toList();
        saveSearchHistoryAsync(keyword,"recipe", recipePage.getTotalElements());
        return buildPageResponse(recipePage, content);
    }

   @Override
    public PageResponse<SearchReponse> searchRecipesByIngredient(String title, List<String> ingredientNames, Pageable pageable) {
       // Ít nhất phải có title hoặc ingredients
       boolean hasTitle = title != null && !title.trim().isEmpty();
       boolean hasIngredients = ingredientNames != null && !ingredientNames.isEmpty();

       if (!hasTitle && !hasIngredients) {
           throw new CustomException(ErrorCode.SEARCH_QUERY_EMPTY);
       }

       // Validate title nếu có
       if (hasTitle) {
           if(title.trim().length() < 2) {
               throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_SHORT);
           }
           if(title.trim().length() > 80) {
               throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_LONG);
           }
           if (!title.trim().matches("^[\\p{L}\\p{N}\\s\\-']+$")) {
               throw new CustomException(ErrorCode.INVALID_CHARACTERS);
           }
       }

       Page<Recipe> recipePage;

       // Bước 1: Tìm kiếm với logic AND (phải có TẤT CẢ nguyên liệu)
       if (hasIngredients) {
           Specification<Recipe> specAll = RecipeSpecification.hasRecipesByIngredients(title, ingredientNames);
           recipePage = recipeRepository.findAll(specAll, pageable);

           // Bước 2: Nếu không tìm thấy kết quả, tìm theo logic OR và sắp xếp theo số lượng khớp
           if (recipePage.isEmpty()) {
               log.info("Không tìm thấy recipe có tất cả nguyên liệu, chuyển sang tìm theo bất kỳ nguyên liệu nào");
               recipePage = searchByAnyIngredientsWithMatchCount(title, ingredientNames, pageable);
           } else {
               log.info("Tìm thấy {} recipe có tất cả nguyên liệu", recipePage.getTotalElements());
           }
       } else {
           // Nếu chỉ có title, tìm theo title thôi
           Specification<Recipe> spec = RecipeSpecification.hasRecipesByIngredients(title, null);
           recipePage = recipeRepository.findAll(spec, pageable);
       }

       List<SearchReponse> content = recipePage.getContent().stream()
               .map(searchMapper::toSearchRecipeResponse)
               .peek(r -> r.setFeaturedImage(firebaseStorageService.convertPathToFirebaseUrl(r.getFeaturedImage())))
               .toList();

       if(!content.isEmpty()) {
           var context=SecurityContextHolder.getContext();
           String name= context.getAuthentication().getName();
           log.info("name: {}", name);
           // Lưu query search: nếu có cả title và ingredients thì kết hợp, không thì lấy cái có
           String searchQuery = hasTitle ? title : String.join(", ", ingredientNames);
           saveSearchHistoryAsync(searchQuery, "ingredient", recipePage.getTotalElements());
       }
       return buildPageResponse(recipePage, content);
    }

    /**
     * Tìm kiếm recipe theo bất kỳ nguyên liệu nào và đếm số lượng nguyên liệu khớp để sắp xếp
     */
    private Page<Recipe> searchByAnyIngredientsWithMatchCount(String title, List<String> ingredientNames, Pageable pageable) {
        // Lọc các ingredient hợp lệ
        List<String> validIngredients = ingredientNames.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(String::trim)
                .toList();

        if (validIngredients.isEmpty()) {
            return Page.empty(pageable);
        }

        // Tìm tất cả recipe có ít nhất 1 nguyên liệu
        Specification<Recipe> specAny = RecipeSpecification.hasRecipesByAnyIngredients(title, validIngredients);
        List<Recipe> allRecipes = recipeRepository.findAll(specAny);

        // Đếm số lượng ingredient khớp cho mỗi recipe
        List<Recipe> sortedRecipes = allRecipes.stream()
                .sorted((r1, r2) -> {
                    int count1 = countMatchingIngredients(r1, validIngredients);
                    int count2 = countMatchingIngredients(r2, validIngredients);
                    return Integer.compare(count2, count1); // Giảm dần (nhiều nhất trước)
                })
                .toList();

        // Phân trang thủ công
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedRecipes.size());
        List<Recipe> pageContent = sortedRecipes.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, sortedRecipes.size());
    }

    /**
     * Đếm số lượng nguyên liệu khớp trong recipe
     */
    private int countMatchingIngredients(Recipe recipe, List<String> ingredientNames) {
        int count = 0;
        for (String searchIngredient : ingredientNames) {
            String searchLower = searchIngredient.toLowerCase();
            boolean found = recipe.getRecipeIngredients().stream()
                    .anyMatch(ri -> ri.getIngredient().getName().toLowerCase().contains(searchLower));
            if (found) {
                count++;
            }
        }
        return count;
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
    private void saveSearchHistoryAsync(String query, String type, long resultCount) {
        var context = SecurityContextHolder.getContext();
        String name= context.getAuthentication().getName();
        Optional<User> user= userRepository.findByUsername(name);
        UUID userId=user.get().getUserId();
        if (userId != null) {
            SearchHistory history = SearchHistory.builder()
                    .userId(userId)
                    .searchQuery(query)
                    .searchType(type)
                    .resultCount((int) resultCount)
                    .build();
            searchHistoryRepository.save(history);

        }
    }
    @Override
    public List<SearchHistoryResponse> getSearchHistory() {
        var context = SecurityContextHolder.getContext();
        String name= context.getAuthentication().getName();
        Optional<User> user= userRepository.findByUsername(name);
        UUID userId=user.get().getUserId();
        List<SearchHistoryResponse> historyResponses=searchHistoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(searchHistoryMapper::toSearchHistoryResponse)
                .toList();
        return historyResponses;
    }
    @Override
    public PageResponse<SearchReponse> searchRecipesByfullName(String keyword, Pageable pageable) {
        if (keyword== null || keyword.trim().isEmpty()) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_EMPTY);
        }
        if(keyword.trim().length() < 2) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_SHORT);
        }
        if(keyword.trim().length() > 80) {
            throw new CustomException(ErrorCode.SEARCH_QUERY_TOO_LONG);
        }
        if (!keyword.trim().matches("^[\\p{L}\\p{N}\\s\\-']+$")) {
            throw new CustomException(ErrorCode.INVALID_CHARACTERS);
        }

        Specification<Recipe> spec = RecipeSpecification.hasRecipeByName(keyword);
        Page<Recipe> recipePage = recipeRepository.findAll(spec, pageable);
        if (recipePage.isEmpty()) {
            Page<User> userPage = userRepository.findByFullNameContainingIgnoreCase(
                    keyword,
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("fullName"))
            );
            if (userPage.isEmpty()) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }

            List<SearchReponse> userResponses = userPage.getContent().stream()
                    .map(searchMapper::toSearchUserResponse)
                    .toList();
            return buildPageResponse(userPage, userResponses);
        }
        List<SearchReponse> content = recipePage.getContent().stream()
                .map(searchMapper::toSearchRecipeResponse)
                .peek(r -> {
                    r.setFeaturedImage(firebaseStorageService.convertPathToFirebaseUrl(r.getFeaturedImage()));
                })
                .toList();
        return buildPageResponse(recipePage, content);
    }
    @Override
    public List<String> getUsernameSuggestions(String query, int limit) {
        String trimmedQuery = query.trim().toLowerCase();

        // Tìm kiếm người dùng có tên chứa từ khóa
        List<User> users = userRepository.findByFullNameContainingIgnoreCase(trimmedQuery)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        // Trả về danh sách tên đầy đủ
        return users.stream()
                .map(User::getFullName)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRecipeSuggestions(String query, int limit) {
        String trimmedQuery = query.trim().toLowerCase();
        List<Recipe> recipes = recipeRepository.findByTitleContainingIgnoreCaseAndStatus(
                        trimmedQuery,
                        RecipeStatus.APPROVED
                )
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
        return recipes.stream()
                .map(Recipe::getTitle)
                .distinct()
                .collect(Collectors.toList());
    }
}
