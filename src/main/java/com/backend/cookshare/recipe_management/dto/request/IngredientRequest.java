package com.backend.cookshare.recipe_management.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IngredientRequest {
    String name;
    String category;
    String unit;
    String description;
    String quantity;
}
