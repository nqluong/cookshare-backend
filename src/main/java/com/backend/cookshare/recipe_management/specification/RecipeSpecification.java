package com.backend.cookshare.recipe_management.specification;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class RecipeSpecification {
    public static Specification<Recipe> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }
            String[] keywords = name.trim().toLowerCase().split("\\s+");

            Predicate predicate = cb.conjunction();
            for (String keyword : keywords) {
                predicate = cb.and(
                        predicate,
                        cb.like(cb.lower(root.get("title")), "%" + keyword + "%")
                );
            }
            return predicate;
        };
    }
    public static Specification<Recipe> hasRecipesByIngredients(String title, List<String> ingredientNames) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            Predicate predicate = criteriaBuilder.conjunction();

            // Lọc theo title nếu có
            if (title != null && !title.trim().isEmpty()) {
                String[] keywords = title.trim().toLowerCase().split("\\s+");
                for (String keyword : keywords) {
                    predicate = criteriaBuilder.and(
                            predicate,
                            criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + keyword + "%")
                    );
                }
            }

            // Lọc theo danh sách ingredients nếu có
            if (ingredientNames != null && !ingredientNames.isEmpty()) {
                for (String ingredientName : ingredientNames) {
                    if (ingredientName != null && !ingredientName.trim().isEmpty()) {
                        Join<Recipe, RecipeIngredient> recipeIngredientJoin = root.join("recipeIngredients", JoinType.INNER);
                        Join<RecipeIngredient, Ingredient> ingredientJoin = recipeIngredientJoin.join("ingredient", JoinType.INNER);

                        predicate = criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(ingredientJoin.get("name")),
                                        "%" + ingredientName.trim().toLowerCase() + "%"
                                )
                        );
                    }
                }
            }

            return predicate;
        };
    }

}
