package com.backend.cookshare.recipe_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngredientDetailRequest {

    @NotNull(message = "ingredientId không được để trống")
    UUID ingredientId;

    @Positive(message = "quantity phải > 0")
    Double quantity;

    @NotBlank(message = "unit không được để trống")
    String unit;

    String notes;
    Integer orderIndex;
}