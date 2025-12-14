package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse;
import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.request.RecipeStepRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.dto.response.RecipeStepResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import com.backend.cookshare.recipe_management.entity.RecipeStep;
import com.backend.cookshare.user.dto.RecipeByFollowingResponse;
import com.backend.cookshare.authentication.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecipeMapperTest {

    private final RecipeMapper mapper = Mappers.getMapper(RecipeMapper.class);

    // =============================
    // toEntity
    // =============================
    @Test
    void toEntity_mapping() {
        RecipeRequest dto = new RecipeRequest();
        dto.setTitle("Recipe");
        dto.setDescription("desc");
        dto.setCookTime(30);

        Recipe entity = mapper.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Recipe", entity.getTitle());

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    // =============================
    // toResponse
    // =============================
    @Test
    void toResponse_basicMapping() {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(UUID.randomUUID());
        recipe.setTitle("R1");
        recipe.setSlug("r1");

        RecipeResponse res = mapper.toResponse(recipe);

        assertNotNull(res);
        assertEquals("R1", res.getTitle());
        assertNull(res.getSteps());
        assertNull(res.getIngredients());
    }

    // =============================
    // updateRecipeFromDto
    // =============================
    @Test
    void updateRecipeFromDto_updatesFields() {
        RecipeRequest dto = new RecipeRequest();
        dto.setTitle("Updated");
        dto.setCookTime(45);

        Recipe recipe = new Recipe();
        recipe.setTitle("Old");

        mapper.updateRecipeFromDto(dto, recipe);

        assertEquals("Updated", recipe.getTitle());
        assertEquals(45, recipe.getCookTime());
    }

    // =============================
    // Step mapping
    // =============================
    @Test
    void stepMapping_entityAndResponse() {
        RecipeStepRequest dto = new RecipeStepRequest();
        dto.setStepNumber(1);
        dto.setInstruction("Mix");

        RecipeStep step = mapper.toStepEntity(dto);
        assertEquals(1, step.getStepNumber());

        RecipeStepResponse res = mapper.toStepResponse(step);
        assertEquals(1, res.getStepNumber());
    }

    // =============================
    // Ingredient mapping
    // =============================
    @Test
    void ingredientMapping() {
        RecipeIngredient ingredient = new RecipeIngredient();
        ingredient.setQuantity("1 tsp");

        RecipeIngredientResponse res = mapper.toIngredientResponse(ingredient);

        assertNotNull(res);
        assertEquals("1 tsp", res.getQuantity());
    }

    // =============================
    // List mapping
    // =============================
    @Test
    void listMapping() {
        RecipeStep step = new RecipeStep();
        step.setStepNumber(1);

        List<RecipeStepResponse> steps =
                mapper.toStepResponseList(List.of(step));

        assertEquals(1, steps.size());

        RecipeIngredient ingredient = new RecipeIngredient();
        List<RecipeIngredientResponse> ingredients =
                mapper.toIngredientResponseList(List.of(ingredient));

        assertEquals(1, ingredients.size());
    }

    // =============================
    // toRecipeByFollowingResponse
    // =============================
    @Test
    void toRecipeByFollowingResponse_mapping() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);

        Recipe recipe = new Recipe();
        recipe.setUser(user);
        recipe.setCreatedAt(LocalDateTime.now());

        RecipeSummaryResponse summary = RecipeSummaryResponse.builder()
                .title("R1")
                .build();

        RecipeByFollowingResponse res =
                mapper.toRecipeByFollowingResponse(recipe, summary);

        assertNotNull(res);
        assertEquals(userId, res.getFollowingId());
        assertEquals("R1", res.getRecipe().getTitle());
    }

    // =============================
    // toRecipeSummary
    // =============================
    @Test
    void toRecipeSummary_nullRecipe() {
        assertNull(mapper.toRecipeSummary(null));
    }

    @Test
    void toRecipeSummary_userNull() {
        Recipe recipe = new Recipe();
        recipe.setRecipeId(UUID.randomUUID());
        recipe.setTitle("R");

        RecipeSummaryResponse res = mapper.toRecipeSummary(recipe);

        assertNotNull(res);
        assertNull(res.getUserId());
        assertEquals("R", res.getTitle());
    }

    @Test
    void toRecipeSummary_userPresent() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUserId(userId);
        user.setUsername("u1");
        user.setFullName("User One");

        Recipe recipe = new Recipe();
        recipe.setRecipeId(UUID.randomUUID());
        recipe.setTitle("Recipe");
        recipe.setUser(user);

        RecipeSummaryResponse res = mapper.toRecipeSummary(recipe);

        assertEquals(userId, res.getUserId());
        assertEquals("u1", res.getUserName());
        assertEquals("User One", res.getFullName());
    }
}
