package com.backend.cookshare.recipe_management.specification;
import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.entity.RecipeIngredient;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RecipeSpecification {

    /**
     * Remove Vietnamese diacritics (ă, â, ê, ô, ơ, ư, đ etc.)
     * Example: "Cơm" -> "Com", "Phở" -> "Pho"
     */
    private static boolean hasVietnameseAccent(String text) {
        String vietnameseAccents = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ" +
                "ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ";

        for (char c : text.toCharArray()) {
            if (vietnameseAccents.indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    public static Specification<Recipe> hasNameLike(String name) {
        return (root, query, cb) -> {
            if (name == null || name.trim().isEmpty()) {
                return cb.conjunction();
            }

            String trimmedName = name.trim().toLowerCase();
            boolean hasAccent = hasVietnameseAccent(trimmedName);
            String[] keywords = trimmedName.split("\\s+");

            Predicate predicate = cb.conjunction();

            if (hasAccent && trimmedName.length() >= 15) {
                // Tìm kiếm cụm từ đầy đủ trước
                Predicate fullPhrasePredicate = cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + trimmedName + "%"),
                        cb.like(cb.lower(root.get("description")), "%" + trimmedName + "%")
                );

                // Tìm kiếm từng từ khóa, yêu cầu ít nhất 60% từ khóa khớp
                int minMatch = (int) Math.ceil(keywords.length * 0.6); // Yêu cầu ít nhất 60% từ khóa khớp
                List<Predicate> keywordPredicates = new ArrayList<>();

                for (String keyword : keywords) {
                    if (keyword.length() >= 2) {
                        Predicate keywordPredicate = cb.or(
                                cb.like(cb.lower(root.get("title")), "%" + keyword + "%"),
                                cb.like(cb.lower(root.get("description")), "%" + keyword + "%")
                        );
                        keywordPredicates.add(keywordPredicate);
                    }
                }
                // Yêu cầu ít nhất minMatch từ khóa khớp
                if (!keywordPredicates.isEmpty()) {
                    predicate = cb.and(
                            predicate,
                            cb.or(fullPhrasePredicate, // Cụm từ đầy đủ khớp
                                    cb.and( // Hoặc ít nhất minMatch từ khóa khớp
                                            keywordPredicates.stream()
                                                    .limit(minMatch)
                                                    .toArray(Predicate[]::new)
                                    )
                            )
                    );
                }

                log.info("Tìm kiếm trong title và description với yêu cầu tối thiểu từ khóa");
                return predicate;
            } else if (hasAccent) {
                // Tìm theo title nếu có dấu và độ dài < 15
                for (String keyword : keywords) {
                    if (keyword.length() >= 2) {
                        predicate = cb.and(
                                predicate,
                                cb.like(cb.lower(root.get("title")), "%" + keyword + "%")
                        );
                    }
                }
                log.info("Tìm kiếm trong title");
                return predicate;
            } else {
                // Tìm theo slug nếu không dấu
                for (String keyword : keywords) {
                    if (keyword.length() >= 2) {
                        predicate = cb.and(
                                predicate,
                                cb.like(
                                        cb.function("replace", String.class,
                                                cb.lower(root.get("slug")),
                                                cb.literal("-"),
                                                cb.literal(" ")
                                        ),
                                        "%" + keyword + "%"
                                )
                        );
                    }
                }
                log.info("Tìm kiếm trong slug");
                return predicate;
            }
        };
    }

    public static Specification<Recipe> hasRecipesByIngredients(String title, List<String> ingredientNames) {
        return (root, query, cb) -> {
            query.distinct(true);

            Predicate predicate = cb.conjunction();

            // Lọc theo slug nếu có
            if (title != null && !title.trim().isEmpty()) {
                String trimmedName = title.toLowerCase();
                boolean hasAccent = hasVietnameseAccent(trimmedName);
                String[] keywords = trimmedName.split("\\s+");
                // Kiểm tra có ký tự tiếng Việt có dấu không
                if (hasAccent && trimmedName.length() >= 15) {
                    // Tìm kiếm cụm từ đầy đủ trước
                    Predicate fullPhrasePredicate = cb.or(
                            cb.like(cb.lower(root.get("title")), "%" + trimmedName + "%"),
                            cb.like(cb.lower(root.get("description")), "%" + trimmedName + "%")
                    );

                    // Tìm kiếm từng từ khóa, yêu cầu ít nhất 60% từ khóa khớp
                    int minMatch = (int) Math.ceil(keywords.length * 0.6); // Yêu cầu ít nhất 60% từ khóa khớp
                    List<Predicate> keywordPredicates = new ArrayList<>();

                    for (String keyword : keywords) {
                        if (keyword.length() >= 2) {
                            Predicate keywordPredicate = cb.or(
                                    cb.like(cb.lower(root.get("title")), "%" + keyword + "%"),
                                    cb.like(cb.lower(root.get("description")), "%" + keyword + "%")
                            );
                            keywordPredicates.add(keywordPredicate);
                        }
                    }

                    // Yêu cầu ít nhất minMatch từ khóa khớp
                    if (!keywordPredicates.isEmpty()) {
                        predicate = cb.and(
                                predicate,
                                cb.or(fullPhrasePredicate, // Cụm từ đầy đủ khớp
                                        cb.and( // Hoặc ít nhất minMatch từ khóa khớp
                                                keywordPredicates.stream()
                                                        .limit(minMatch)
                                                        .toArray(Predicate[]::new)
                                        )
                                )
                        );
                    }

                    log.info("Tìm kiếm trong title và description với yêu cầu tối thiểu từ khóa");
                    return predicate;
                } else if (hasAccent) {
                    // Tìm theo title nếu có dấu và độ dài < 15
                    for (String keyword : keywords) {
                        if (keyword.length() >= 2) {
                            predicate = cb.and(
                                    predicate,
                                    cb.like(cb.lower(root.get("title")), "%" + keyword + "%")
                            );
                        }
                    }
                    log.info("Tìm kiếm trong title");
                    return predicate;
                } else {
                    // Tìm theo slug nếu không dấu
                    for (String keyword : keywords) {
                        if (keyword.length() >= 2) {
                            predicate = cb.and(
                                    predicate,
                                    cb.like(
                                            cb.function("replace", String.class,
                                                    cb.lower(root.get("slug")),
                                                    cb.literal("-"),
                                                    cb.literal(" ")
                                            ),
                                            "%" + keyword + "%"
                                    )
                            );
                        }
                    }
                    log.info("Tìm kiếm trong slug");
                    return predicate;
                }
            }

            if (ingredientNames != null && !ingredientNames.isEmpty()) {
                for (String ingredientName : ingredientNames) {
                    if (ingredientName != null && !ingredientName.trim().isEmpty()) {
                        Join<Recipe, RecipeIngredient> recipeIngredientJoin = root.join("recipeIngredients", JoinType.INNER);
                        Join<RecipeIngredient, Ingredient> ingredientJoin = recipeIngredientJoin.join("ingredient", JoinType.INNER);

                        predicate = cb.and(
                                predicate,
                                cb.like(
                                        cb.lower(ingredientJoin.get("name")),
                                        "%" + ingredientName.trim().toLowerCase() + "%"
                                )
                        );
                    }
                }
            }

            return predicate;
        };
    }
    public static Specification<Recipe> hasRecipeByName(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            String trimmedName = name.trim().toLowerCase();
            Join<Recipe, User> userJoin = root.join("user", JoinType.INNER);
            Predicate predicate = criteriaBuilder.like(
                    criteriaBuilder.lower(userJoin.get("fullName")),
                    "%" + trimmedName + "%"
            );

            log.info("Tìm kiếm công thức theo tên người tạo: {}", trimmedName);
            return predicate;
        };
    }

}