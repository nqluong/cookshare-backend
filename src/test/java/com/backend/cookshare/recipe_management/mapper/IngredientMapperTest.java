package com.backend.cookshare.recipe_management.mapper;

import com.backend.cookshare.recipe_management.dto.request.IngredientRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeIngredientResponse;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IngredientMapperTest {

    private final IngredientMapper mapper =
            Mappers.getMapper(IngredientMapper.class);

    // =========================
    // toEntity
    // =========================
    @Test
    void toEntity_fullMapping() {
        IngredientRequest request = new IngredientRequest();
        request.setName("Sugar");
        request.setUnit("gram");

        Ingredient entity = mapper.toEntity(request);

        assertNotNull(entity);
        assertEquals("Sugar", entity.getName());
        assertEquals("gram", entity.getUnit());
    }

    // =========================
    // toResponse
    // =========================
    @Test
    void toResponse_fullMapping() {
        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientId(UUID.randomUUID());
        ingredient.setName("Salt");
        ingredient.setUnit("tsp");

        RecipeIngredientResponse response =
                mapper.toResponse(ingredient);

        assertNotNull(response);
        assertEquals("Salt", response.getName());
        assertEquals("tsp", response.getUnit());
    }

    // =========================
    // updateIngredientFromDto
    // =========================
    @Test
    void updateIngredientFromDto_updateAllFields() {
        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientId(UUID.randomUUID());
        ingredient.setName("Old name");
        ingredient.setUnit("kg");

        IngredientRequest request = new IngredientRequest();
        request.setName("Flour");
        request.setUnit("gram");

        mapper.updateIngredientFromDto(request, ingredient);

        assertEquals("Flour", ingredient.getName());
        assertEquals("gram", ingredient.getUnit());
    }

    @Test
    void updateIngredientFromDto_ignoreNullFields() {
        Ingredient ingredient = new Ingredient();
        ingredient.setIngredientId(UUID.randomUUID());
        ingredient.setName("Butter");
        ingredient.setUnit("gram");

        IngredientRequest request = new IngredientRequest();
        request.setName(null);
        request.setUnit("kg");

        mapper.updateIngredientFromDto(request, ingredient);

        // name không bị overwrite vì null
        assertEquals("Butter", ingredient.getName());
        assertEquals("kg", ingredient.getUnit());
    }

    // =========================
    // edge cases
    // =========================
    @Test
    void toEntity_nullInput() {
        Ingredient entity = mapper.toEntity(null);
        assertNull(entity);
    }

    @Test
    void toResponse_nullInput() {
        RecipeIngredientResponse res =
                mapper.toResponse(null);
        assertNull(res);
    }
}
