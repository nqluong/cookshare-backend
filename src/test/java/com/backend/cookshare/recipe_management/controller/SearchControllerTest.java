package com.backend.cookshare.recipe_management.controller;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.interaction.dto.response.SearchHistoryResponse;
import com.backend.cookshare.recipe_management.dto.response.IngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import com.backend.cookshare.recipe_management.service.SearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController).build();
    }

    // ================= searchRecipes =================
    @Test
    void searchRecipes_success_ASC() throws Exception {
        PageResponse<SearchReponse> pageResponse =
                PageResponse.<SearchReponse>builder()
                        .content(List.of(SearchReponse.builder().title("Recipe").build()))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .build();

        when(searchService.searchRecipesByName(eq("cake"), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/searchs/recipe")
                        .param("title", "cake")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].title").value("Recipe"));
    }

    @Test
    void searchRecipes_success_DESC() throws Exception {
        when(searchService.searchRecipesByName(anyString(), any(Pageable.class)))
                .thenReturn(new PageResponse<>());

        mockMvc.perform(get("/searchs/recipe")
                        .param("title", "cake")
                        .param("direction", "DESC"))
                .andExpect(status().isOk());
    }

    // ================= search by ingredient =================
    @Test
    void searchRecipesByIngredient_success() throws Exception {
        when(searchService.searchRecipesByIngredient(any(), any(), any()))
                .thenReturn(new PageResponse<>());

        mockMvc.perform(get("/searchs/recipebyingredient")
                        .param("title", "cake")
                        .param("ingredients", "egg", "milk"))
                .andExpect(status().isOk());
    }

    // ================= top ingredients =================
    @Test
    void top10MostIngredients_success() throws Exception {
        when(searchService.top10MostUsedIngredients())
                .thenReturn(List.of(
                        IngredientResponse.builder().name("Salt").recipeCount(10).build()
                ));

        mockMvc.perform(get("/searchs/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].name").value("Salt"));
    }

    // ================= history =================
    @Test
    void getSearchHistory_success() throws Exception {
        when(searchService.getSearchHistory())
                .thenReturn(List.of(new SearchHistoryResponse()));

        mockMvc.perform(get("/searchs/history"))
                .andExpect(status().isOk());
    }

    // ================= search user =================
    @Test
    void searchUser_success() throws Exception {
        when(searchService.searchRecipesByfullName(anyString(), any()))
                .thenReturn(new PageResponse<>());

        mockMvc.perform(get("/searchs/user")
                        .param("name", "John"))
                .andExpect(status().isOk());
    }

    // ================= user suggestions =================
    @Test
    void getUserSuggestions_queryTooShort() throws Exception {
        mockMvc.perform(get("/searchs/user/suggestions")
                        .param("query", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Query too short"))
                .andExpect(jsonPath("$.result").isArray());
    }

    @Test
    void getUserSuggestions_success() throws Exception {
        when(searchService.getUsernameSuggestions("john", 5))
                .thenReturn(List.of("john123"));

        mockMvc.perform(get("/searchs/user/suggestions")
                        .param("query", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0]").value("john123"));
    }

    // ================= recipe suggestions =================
    @Test
    void getRecipeSuggestions_queryTooShort() throws Exception {
        mockMvc.perform(get("/searchs/recipe/suggestions")
                        .param("query", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Query too short"));
    }

    @Test
    void getRecipeSuggestions_success() throws Exception {
        when(searchService.getRecipeSuggestions("cake", 5))
                .thenReturn(List.of("Chocolate Cake"));

        mockMvc.perform(get("/searchs/recipe/suggestions")
                        .param("query", "cake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0]").value("Chocolate Cake"));
    }
}
