package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.recipe_management.dto.response.IngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.SearchReponse;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.entity.Recipe;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SearchMapperTest {

    private final SearchMapper mapper = Mappers.getMapper(SearchMapper.class);

    // ===============================
    // toSearchRecipeResponse
    // ===============================
    @Test
    void toSearchRecipeResponse_fullMapping() {
        UUID recipeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);
        user.setFullName("John Doe");
        user.setAvatarUrl("avatar.png");

        Recipe recipe = new Recipe();
        recipe.setRecipeId(recipeId);
        recipe.setTitle("Recipe Title");
        recipe.setSlug("recipe-title");
        recipe.setDescription("desc");
        recipe.setFeaturedImage("img.png");
        recipe.setCookTime(30);
        recipe.setViewCount(100);
        recipe.setLikeCount(10);
        recipe.setSaveCount(5);
        recipe.setUser(user);

        SearchReponse res = mapper.toSearchRecipeResponse(recipe);

        assertNotNull(res);
        assertEquals(recipeId, res.getRecipeId());
        assertEquals("Recipe Title", res.getTitle());
        assertEquals("recipe-title", res.getSlug());
        assertEquals("John Doe", res.getFullName());
        assertEquals("avatar.png", res.getAvatarUrl());
    }

    // ===============================
    // toIngredientResponse
    // ===============================
    @Test
    void toIngredientResponse_mapping() {
        UUID ingredientId = UUID.randomUUID();

        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientId(ingredientId);
        ingredient.setName("Salt");

        IngredientResponse res = mapper.toIngredientResponse(ingredient);

        assertNotNull(res);
        assertEquals(ingredientId, res.getIngredientId());
        assertEquals("Salt", res.getName());
    }

    // ===============================
    // toIngredientResponseFromArray
    // ===============================
    @Test
    void toIngredientResponseFromArray_nullRow() {
        IngredientResponse res = mapper.toIngredientResponseFromArray(null);
        assertNull(res);
    }

    @Test
    void toIngredientResponseFromArray_invalidLength() {
        Object[] row = new Object[] { "id", "name" };
        IngredientResponse res = mapper.toIngredientResponseFromArray(row);
        assertNull(res);
    }

    @Test
    void toIngredientResponseFromArray_valid() {
        UUID ingredientId = UUID.randomUUID();
        Object[] row = new Object[] {
                ingredientId,
                "Sugar",
                12L
        };

        IngredientResponse res = mapper.toIngredientResponseFromArray(row);

        assertNotNull(res);
        assertEquals(ingredientId, res.getIngredientId());
        assertEquals("Sugar", res.getName());
        assertEquals(12, res.getRecipeCount());
    }

    // ===============================
    // toSearchUserResponse
    // ===============================
    @Test
    void toSearchUserResponse_nullUser() {
        SearchReponse res = mapper.toSearchUserResponse(null);
        assertNull(res);
    }

    @Test
    void toSearchUserResponse_valid() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);
        user.setFullName("Alice");
        user.setAvatarUrl("alice.png");

        SearchReponse res = mapper.toSearchUserResponse(user);

        assertNotNull(res);
        assertEquals(userId, res.getUserId());
        assertEquals("Alice", res.getFullName());
        assertEquals("alice.png", res.getAvatarUrl());
        assertNull(res.getRecipeId());
    }
}
