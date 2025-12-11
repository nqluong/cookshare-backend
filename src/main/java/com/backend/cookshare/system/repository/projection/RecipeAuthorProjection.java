package com.backend.cookshare.system.repository.projection;

import java.util.UUID;

/**
 * Projection chứa thông tin tác giả của Recipe.
 */
public interface RecipeAuthorProjection {
    UUID getAuthorId();
    String getAuthorUsername();
    String getAuthorFullName();
}
