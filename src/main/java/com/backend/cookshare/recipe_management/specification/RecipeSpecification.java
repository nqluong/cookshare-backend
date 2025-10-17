package com.backend.cookshare.recipe_management.specification;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.List;

public class RecipeSpecification {

    /**
     * Remove Vietnamese diacritics (ă, â, ê, ô, ơ, ư, đ etc.)
     * Example: "Cơm" -> "Com", "Phở" -> "Pho"
     */
    public static String removeAccents(String str) {
        if (str == null) {
            return null;
        }
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{M}", "").toLowerCase();
    }

    public static Specification<Recipe> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }

            String unaccentedName = removeAccents(name.trim());
            String[] keywords = unaccentedName.split("\\s+");

            Predicate predicate = cb.conjunction();
            for (String keyword : keywords) {
                // Compare unaccented slug with unaccented keyword
                predicate = cb.and(
                        predicate,
                        cb.like(
                                cb.function("replace", String.class,
                                        cb.function("lower", String.class, root.get("slug")),
                                        cb.literal("-"),
                                        cb.literal(" ")
                                ),
                                "%" + keyword + "%"
                        )
                );
            }
            return predicate;
        };
    }

    public static Specification<Recipe> hasRecipesByIngredients(String title, List<String> ingredientNames) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            Predicate predicate = criteriaBuilder.conjunction();

            // Lọc theo slug nếu có
            if (title != null && !title.trim().isEmpty()) {
                String unaccentedTitle = removeAccents(title.trim());
                String[] keywords = unaccentedTitle.split("\\s+");
                for (String keyword : keywords) {
                    predicate = criteriaBuilder.and(
                            predicate,
                            criteriaBuilder.like(
                                    criteriaBuilder.function("replace", String.class,
                                            criteriaBuilder.function("lower", String.class, root.get("slug")),
                                            criteriaBuilder.literal("-"),
                                            criteriaBuilder.literal(" ")
                                    ),
                                    "%" + keyword + "%"
                            )
                    );
                }
            }

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