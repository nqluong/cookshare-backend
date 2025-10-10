package com.backend.cookshare.recipe_management.specification;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
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
    public static Specification<Recipe> hasRecipesByIngredient(String ingredient) {
        return (root, query, criteriaBuilder) -> {
            if (ingredient == null || ingredient.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Join<Recipe, RecipeIngredient> recipeIngredientJoin = root.join("recipeIngredients", JoinType.INNER);
            Join<RecipeIngredient, Ingredient> ingredientJoin = recipeIngredientJoin.join("ingredient", JoinType.INNER);
            String[] keywords = ingredient.trim().toLowerCase().split("\\s+");
            Predicate predicate = criteriaBuilder.conjunction();
            for (String keyword: keywords) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(ingredientJoin.get("name")), "%" + keyword + "%")
                );
            }
            return predicate;
        };
    }

}
