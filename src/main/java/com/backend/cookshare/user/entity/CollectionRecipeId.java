package com.backend.cookshare.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRecipeId implements Serializable {
    private UUID collectionId;
    private UUID recipeId;
}
