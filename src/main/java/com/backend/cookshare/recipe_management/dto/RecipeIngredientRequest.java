package com.backend.cookshare.recipe_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecipeIngredientRequest {

    @NotNull(message = "ingredientId không được để trống")
    UUID ingredientId;

    @NotBlank(message = "quantity không được để trống")
    String quantity;

    String unit;
    String notes;

    @PositiveOrZero(message = "orderIndex phải >= 0")
    Integer orderIndex;
}
