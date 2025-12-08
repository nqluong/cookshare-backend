package com.backend.cookshare.system.repository.projection;

import java.util.UUID;

public interface RecipeTitleProjection {
    UUID getRecipeId();
    String getTitle();
}
