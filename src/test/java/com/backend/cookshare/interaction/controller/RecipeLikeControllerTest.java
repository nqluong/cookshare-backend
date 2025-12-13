package com.backend.cookshare.interaction.controller;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.interaction.dto.request.RecipeLikeRequest;
import com.backend.cookshare.interaction.dto.response.RecipeLikeResponse;
import com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse;
import com.backend.cookshare.interaction.sevice.RecipeLikeService;
import com.backend.cookshare.recipe_management.dto.ApiResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeLikeControllerTest {

    @Mock
    private RecipeLikeService recipeLikeService;

    @InjectMocks
    private RecipeLikeController recipeLikeController;

    private UUID recipeId;
    private UUID userId;
    private RecipeLikeResponse likeResponse;

    @BeforeEach
    void setUp() {
        recipeId = UUID.randomUUID();
        userId = UUID.randomUUID();

        likeResponse = RecipeLikeResponse.builder()
                .userId(userId)
                .recipeId(recipeId)
                .createdAt(LocalDateTime.now())
                .recipe(new RecipeSummaryResponse())
                .build();
    }

    @Test
    void likerecipe_ShouldReturnSuccessResponse() {
        RecipeLikeRequest request = new RecipeLikeRequest();
        request.setRecipeId(recipeId);

        when(recipeLikeService.likerecipe(recipeId)).thenReturn(likeResponse);

        ApiResponse<RecipeLikeResponse> response = recipeLikeController.likerecipe(request);

        assertNotNull(response);
        assertEquals(recipeId, response.getResult().getRecipeId());
        assertEquals(userId, response.getResult().getUserId());

        verify(recipeLikeService).likerecipe(recipeId);
    }

    @Test
    void isRecipeLiked_ShouldReturnBoolean() {
        when(recipeLikeService.isRecipeLiked(recipeId)).thenReturn(true);

        ApiResponse<Boolean> response = recipeLikeController.isRecipeLiked(recipeId);

        assertNotNull(response);
        assertTrue(response.getResult());
        verify(recipeLikeService).isRecipeLiked(recipeId);
    }

    @Test
    void unlikeRecipe_ShouldCallServiceAndReturnMessage() {
        ApiResponse<String> response = recipeLikeController.unlikeRecipe(recipeId);

        assertNotNull(response);
        assertEquals("Unliked thành công", response.getResult());
        verify(recipeLikeService).unlikerecipe(recipeId);
    }

    @Test
    void getAllLikedList_ShouldReturnPagedResults() {
        int page = 0, size = 10;

        PageResponse<RecipeLikeResponse> mockPage = PageResponse.<RecipeLikeResponse>builder()
                .content(List.of(likeResponse))
                .page(page)
                .size(size)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .empty(false)
                .numberOfElements(1)
                .sorted(false)
                .build();

        when(recipeLikeService.getallRecipeLiked(page, size)).thenReturn(mockPage);

        ApiResponse<PageResponse<RecipeLikeResponse>> response =
                recipeLikeController.getAllLikedList(page, size);

        assertNotNull(response);
        assertEquals(1, response.getResult().getTotalElements());
        assertEquals(1, response.getResult().getNumberOfElements());

        verify(recipeLikeService).getallRecipeLiked(page, size);
    }

    @Test
    void checkMultipleLikes_ShouldReturnMapOfResults() {
        List<UUID> recipeIds = List.of(recipeId, UUID.randomUUID());

        Map<UUID, Boolean> resultMap = new HashMap<>();
        resultMap.put(recipeIds.get(0), true);
        resultMap.put(recipeIds.get(1), false);

        when(recipeLikeService.checkMultipleLikes(recipeIds)).thenReturn(resultMap);

        ApiResponse<Map<UUID, Boolean>> response =
                recipeLikeController.checkMultipleLikes(recipeIds);

        assertNotNull(response);
        assertEquals(2, response.getResult().size());
        assertTrue(response.getResult().get(recipeIds.get(0)));
        assertFalse(response.getResult().get(recipeIds.get(1)));

        verify(recipeLikeService).checkMultipleLikes(recipeIds);
    }
}
