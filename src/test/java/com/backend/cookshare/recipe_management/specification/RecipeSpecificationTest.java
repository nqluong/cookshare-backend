//package com.backend.cookshare.recipe_management.specification;
//
//import com.backend.cookshare.authentication.entity.User;
//import com.backend.cookshare.recipe_management.entity.Recipe;
//import jakarta.persistence.criteria.*;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.util.List;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class RecipeSpecificationTest {
//
//    @Mock
//    Root<Recipe> root;
//
//    @Mock
//    CriteriaQuery<?> query;
//
//    @Mock
//    CriteriaBuilder cb;
//
//    @BeforeEach
//    void setup() {
//        // distinct(true) trả lại query để tránh null
//        doReturn(query).when(query).distinct(anyBoolean());
//
//        // mock chung cho predicate
//        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
//        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
//        when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
//        when(cb.lower(any())).thenReturn(mock(Expression.class));
//    }
//
//    void execute(Specification<Recipe> spec) {
//        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
//    }
//
//    // =====================================================
//    // hasNameLike
//    // =====================================================
//    @Nested
//    class HasNameLikeTests {
//
//        static Stream<String> validNames() {
//            return Stream.of(
//                    "com tam suon",
//                    "pho bo",
//                    "cơm tấm",
//                    "phở bò",
//                    "cơm tấm sườn nướng ngon tuyệt vời tại sài gòn",
//                    "bánh mì pate gan gà ngon nhất"
//            );
//        }
//
//        @ParameterizedTest
//        @MethodSource("validNames")
//        void hasNameLike_validInput_shouldNotThrow(String name) {
//            Specification<Recipe> spec = RecipeSpecification.hasNameLike(name);
//
//            doReturn(mock(Path.class)).when(root).get("title");
//            doReturn(mock(Path.class)).when(root).get("description");
//            doReturn(mock(Path.class)).when(root).get("slug");
//
//            when(cb.function(eq("replace"), eq(String.class), any(), any(), any()))
//                    .thenReturn(mock(Expression.class));
//
//            execute(spec);
//        }
//
//        @Test
//        void hasNameLike_nullOrBlank_shouldNotThrow() {
//            execute(RecipeSpecification.hasNameLike(null));
//            execute(RecipeSpecification.hasNameLike(""));
//            execute(RecipeSpecification.hasNameLike("   "));
//        }
//    }
//
//    // =====================================================
//    // hasRecipesByIngredients
//    // =====================================================
//    @Nested
//    class HasRecipesByIngredientsTests {
//
//        @Test
//        void onlyIngredients_shouldDistinctAndNotThrow() {
//            Specification<Recipe> spec =
//                    RecipeSpecification.hasRecipesByIngredients(null, List.of("thịt bò", "hành"));
//
//            Join<?, ?> riJoin = mock(Join.class);
//            Join<?, ?> ingJoin = mock(Join.class);
//            Path<String> namePath = mock(Path.class);
//
//            doReturn(riJoin).when(root).join("recipeIngredients", JoinType.INNER);
//            doReturn(ingJoin).when(riJoin).join("ingredient", JoinType.INNER);
//            doReturn(namePath).when(ingJoin).get("name");
//
//            execute(spec);
//
//            verify(query).distinct(true);
//        }
//
//        @Test
//        void titleAndIngredients_shouldNotThrow() {
//            Specification<Recipe> spec =
//                    RecipeSpecification.hasRecipesByIngredients("phở bò", List.of("bánh phở"));
//
//            doReturn(mock(Path.class)).when(root).get("title");
//            doReturn(mock(Join.class)).when(root).join(eq("recipeIngredients"), any());
//
//            execute(spec);
//        }
//
//        @Test
//        void emptyCases_shouldNotThrow() {
//            execute(RecipeSpecification.hasRecipesByIngredients("test", null));
//            execute(RecipeSpecification.hasRecipesByIngredients("test", List.of()));
//            execute(RecipeSpecification.hasRecipesByIngredients(null, List.of("gà")));
//        }
//    }
//
//    // =====================================================
//    // hasRecipeByName
//    // =====================================================
//    @Nested
//    class HasRecipeByNameTests {
//
//        @Test
//        void validName_shouldJoinUser() {
//            Specification<Recipe> spec =
//                    RecipeSpecification.hasRecipeByName("Nguyễn Văn A");
//
//            @SuppressWarnings("unchecked")
//            Join<Recipe, User> userJoin = (Join<Recipe, User>) mock(Join.class);
//
//            @SuppressWarnings("unchecked")
//            Path<String> fullNamePath = (Path<String>) mock(Path.class);
//
//            doReturn(userJoin).when(root).join("user", JoinType.INNER);
//            doReturn(fullNamePath).when(userJoin).get("fullName");
//
//            execute(spec);
//
//            verify(root).join("user", JoinType.INNER);
//        }
//
//        @Test
//        void nullOrBlank_shouldNotJoin() {
//            execute(RecipeSpecification.hasRecipeByName(null));
//            execute(RecipeSpecification.hasRecipeByName("   "));
//
//            verify(root, never()).join(anyString(), any());
//        }
//    }
//
//    // =====================================================
//    // hasVietnameseAccent (helper)
//    // =====================================================
//    @Test
//    void hasVietnameseAccent_helper() throws Exception {
//        var method =
//                RecipeSpecification.class.getDeclaredMethod("hasVietnameseAccent", String.class);
//        method.setAccessible(true);
//
//        assertTrue((Boolean) method.invoke(null, "phở"));
//        assertTrue((Boolean) method.invoke(null, "Đặng"));
//        assertFalse((Boolean) method.invoke(null, "hello"));
//        assertFalse((Boolean) method.invoke(null, ""));
//        assertFalse((Boolean) method.invoke(null, "abc123"));
//    }
//}
