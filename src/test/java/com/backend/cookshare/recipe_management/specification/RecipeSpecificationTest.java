package com.backend.cookshare.recipe_management.specification;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.recipe_management.entity.Recipe;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeSpecificationTest {

    @Mock
    Root<Recipe> root;

    @Mock
    CriteriaQuery<?> query;

    @Mock
    CriteriaBuilder cb;

    @BeforeEach
    void setup() {
        lenient().doReturn(query).when(query).distinct(anyBoolean());
        lenient().when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));
    }

    void execute(Specification<Recipe> spec) {
        assertDoesNotThrow(() -> spec.toPredicate(root, query, cb));
    }

    // =====================================================
    // hasNameLike
    // =====================================================
    @Nested
    class HasNameLikeTests {

        static Stream<String> validNames() {
            return Stream.of(
                    "com tam suon",
                    "pho bo",
                    "com tam",
                    "pho bo"
            );
        }

        static Stream<String> longVietnameseNames() {
            return Stream.of(
                    "cơm tấm sườn nướng ngon tuyệt vời tại sài gòn",
                    "bánh mì pate gan gà ngon nhất việt nam",
                    "phở bò Nam Định truyền thống hương vị đậm đà"
            );
        }

        static Stream<String> shortVietnameseNames() {
            return Stream.of(
                    "phở bò",
                    "cơm tấm",
                    "bánh mì",
                    "bún chả"
            );
        }

        @ParameterizedTest
        @MethodSource("validNames")
        void hasNameLike_noAccent_shouldSearchInSlug(String name) {
            lenient().doReturn(mock(Path.class)).when(root).get("title");
            lenient().doReturn(mock(Path.class)).when(root).get("description");
            lenient().doReturn(mock(Path.class)).when(root).get("slug");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.conjunction()).thenReturn(mock(Predicate.class));
            lenient().when(cb.function(eq("replace"), eq(String.class), any(), any(), any()))
                    .thenReturn(mock(Expression.class));

            Specification<Recipe> spec = RecipeSpecification.hasNameLike(name);
            execute(spec);
        }

        @ParameterizedTest
        @MethodSource("longVietnameseNames")
        void hasNameLike_longVietnameseText_shouldSearchInTitleAndDescription(String name) {
            lenient().doReturn(mock(Path.class)).when(root).get("title");
            lenient().doReturn(mock(Path.class)).when(root).get("description");
            lenient().doReturn(mock(Path.class)).when(root).get("slug");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.or(any(Predicate[].class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.conjunction()).thenReturn(mock(Predicate.class));

            Specification<Recipe> spec = RecipeSpecification.hasNameLike(name);
            execute(spec);
        }

        @ParameterizedTest
        @MethodSource("shortVietnameseNames")
        void hasNameLike_shortVietnameseText_shouldSearchInTitle(String name) {
            lenient().doReturn(mock(Path.class)).when(root).get("title");
            lenient().doReturn(mock(Path.class)).when(root).get("description");
            lenient().doReturn(mock(Path.class)).when(root).get("slug");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.conjunction()).thenReturn(mock(Predicate.class));

            Specification<Recipe> spec = RecipeSpecification.hasNameLike(name);
            execute(spec);
        }

        @Test
        void hasNameLike_singleCharacterKeyword_shouldSkip() {
            lenient().doReturn(mock(Path.class)).when(root).get("title");
            lenient().doReturn(mock(Path.class)).when(root).get("slug");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.conjunction()).thenReturn(mock(Predicate.class));
            lenient().when(cb.function(eq("replace"), eq(String.class), any(), any(), any()))
                    .thenReturn(mock(Expression.class));

            // Test với từ khóa có 1 ký tự (sẽ bị skip do length < 2)
            execute(RecipeSpecification.hasNameLike("a b c"));
        }

        @Test
        void hasNameLike_nullOrBlank_shouldReturnConjunction() {
            lenient().when(cb.conjunction()).thenReturn(mock(Predicate.class));
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            execute(RecipeSpecification.hasNameLike(null));
            execute(RecipeSpecification.hasNameLike(""));
            execute(RecipeSpecification.hasNameLike("   "));
        }
    }

    // =====================================================
    // hasRecipesByIngredients
    // =====================================================
    @Nested
    class HasRecipesByIngredientsTests {

        @Test
        void onlyIngredients_shouldDistinctAndJoin() {
            Join<?, ?> riJoin = mock(Join.class);
            Join<?, ?> ingJoin = mock(Join.class);
            Path<String> namePath = mock(Path.class);

            lenient().doReturn(riJoin).when(root).join("recipeIngredients", JoinType.INNER);
            lenient().doReturn(ingJoin).when(riJoin).join("ingredient", JoinType.INNER);
            lenient().doReturn(namePath).when(ingJoin).get("name");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));

            Specification<Recipe> spec =
                    RecipeSpecification.hasRecipesByIngredients(null, List.of("thịt bò", "hành"));

            execute(spec);

            verify(query).distinct(true);
        }

        @Test
        void longTitleWithIngredients_shouldSearchTitleDescriptionAndIngredients() {
            Join<?, ?> riJoin = mock(Join.class);
            Join<?, ?> ingJoin = mock(Join.class);
            Path<String> namePath = mock(Path.class);

            lenient().doReturn(riJoin).when(root).join("recipeIngredients", JoinType.INNER);
            lenient().doReturn(ingJoin).when(riJoin).join("ingredient", JoinType.INNER);
            lenient().doReturn(namePath).when(ingJoin).get("name");
            lenient().doReturn(mock(Path.class)).when(root).get("title");
            lenient().doReturn(mock(Path.class)).when(root).get("description");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.or(any(Predicate[].class))).thenReturn(mock(Predicate.class));

            Specification<Recipe> spec = RecipeSpecification.hasRecipesByIngredients(
                    "cơm tấm sườn nướng ngon tuyệt vời tại sài gòn",
                    List.of("thịt sườn", "nước mắm")
            );

            execute(spec);
        }

        @Test
        void shortTitleWithIngredients_shouldSearchTitleAndIngredients() {
            Join<?, ?> riJoin = mock(Join.class);
            Join<?, ?> ingJoin = mock(Join.class);
            Path<String> namePath = mock(Path.class);

            lenient().doReturn(riJoin).when(root).join("recipeIngredients", JoinType.INNER);
            lenient().doReturn(ingJoin).when(riJoin).join("ingredient", JoinType.INNER);
            lenient().doReturn(namePath).when(ingJoin).get("name");
            lenient().doReturn(mock(Path.class)).when(root).get("title");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));

            Specification<Recipe> spec = RecipeSpecification.hasRecipesByIngredients(
                    "phở bò",
                    List.of("bánh phở")
            );

            execute(spec);
        }

        @Test
        void noAccentTitleWithIngredients_shouldSearchSlugAndIngredients() {
            Join<?, ?> riJoin = mock(Join.class);
            Join<?, ?> ingJoin = mock(Join.class);
            Path<String> namePath = mock(Path.class);

            lenient().doReturn(riJoin).when(root).join("recipeIngredients", JoinType.INNER);
            lenient().doReturn(ingJoin).when(riJoin).join("ingredient", JoinType.INNER);
            lenient().doReturn(namePath).when(ingJoin).get("name");
            lenient().doReturn(mock(Path.class)).when(root).get("slug");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.function(eq("replace"), eq(String.class), any(), any(), any()))
                    .thenReturn(mock(Expression.class));

            Specification<Recipe> spec = RecipeSpecification.hasRecipesByIngredients(
                    "pho bo",
                    List.of("beef")
            );

            execute(spec);
        }

        @Test
        void onlyTitle_noIngredients_shouldSearchTitle() {
            lenient().doReturn(mock(Path.class)).when(root).get("slug");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
            lenient().when(cb.function(eq("replace"), eq(String.class), any(), any(), any()))
                    .thenReturn(mock(Expression.class));

            execute(RecipeSpecification.hasRecipesByIngredients("test recipe", null));
            execute(RecipeSpecification.hasRecipesByIngredients("test recipe", List.of()));
        }

        @Test
        void nullTitle_withIngredients_shouldSearchIngredients() {
            Join<?, ?> riJoin = mock(Join.class);
            Join<?, ?> ingJoin = mock(Join.class);
            Path<String> namePath = mock(Path.class);

            lenient().doReturn(riJoin).when(root).join("recipeIngredients", JoinType.INNER);
            lenient().doReturn(ingJoin).when(riJoin).join("ingredient", JoinType.INNER);
            lenient().doReturn(namePath).when(ingJoin).get("name");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));

            execute(RecipeSpecification.hasRecipesByIngredients(null, List.of("gà", "hành")));
        }

        @Test
        void emptyIngredientName_shouldSkip() {
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));

            // Test với list chứa empty và blank strings (không thể dùng null trong List.of)
            execute(RecipeSpecification.hasRecipesByIngredients(null, List.of("", "   ")));

            // Test với null list
            execute(RecipeSpecification.hasRecipesByIngredients(null, null));
        }
    }

    // =====================================================
    // hasRecipeByName
    // =====================================================
    @Nested
    class HasRecipeByNameTests {

        @Test
        void validName_shouldJoinUserAndSearch() {
            @SuppressWarnings("unchecked")
            Join<Recipe, User> userJoin = (Join<Recipe, User>) mock(Join.class);

            @SuppressWarnings("unchecked")
            Path<String> fullNamePath = (Path<String>) mock(Path.class);

            lenient().doReturn(userJoin).when(root).join("user", JoinType.INNER);
            lenient().doReturn(fullNamePath).when(userJoin).get("fullName");
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            lenient().when(cb.lower(any())).thenReturn(mock(Expression.class));
            lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
            lenient().when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));

            Specification<Recipe> spec =
                    RecipeSpecification.hasRecipeByName("Nguyễn Văn A");

            execute(spec);

            verify(root).join("user", JoinType.INNER);
            verify(cb).like(any(), eq("%nguyễn văn a%"));
        }

        @Test
        void nullName_shouldNotJoin() {
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            execute(RecipeSpecification.hasRecipeByName(null));

            verify(root, never()).join(anyString(), any());
        }

        @Test
        void blankName_shouldNotJoin() {
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            execute(RecipeSpecification.hasRecipeByName("   "));

            verify(root, never()).join(anyString(), any());
        }

        @Test
        void emptyName_shouldNotJoin() {
            lenient().doReturn(mock(Path.class)).when(root).get("status");

            execute(RecipeSpecification.hasRecipeByName(""));

            verify(root, never()).join(anyString(), any());
        }
    }

    // =====================================================
    // hasVietnameseAccent (helper)
    // =====================================================
    @Nested
    class HasVietnameseAccentTests {

        @Test
        void withVietnameseAccents_shouldReturnTrue() throws Exception {
            var method =
                    RecipeSpecification.class.getDeclaredMethod("hasVietnameseAccent", String.class);
            method.setAccessible(true);

            assertTrue((Boolean) method.invoke(null, "phở"));
            assertTrue((Boolean) method.invoke(null, "đặng"));
            assertTrue((Boolean) method.invoke(null, "cơm"));
            assertTrue((Boolean) method.invoke(null, "tấm"));
            assertTrue((Boolean) method.invoke(null, "bánh"));
            assertTrue((Boolean) method.invoke(null, "mì"));
            assertTrue((Boolean) method.invoke(null, "sườn"));
            assertTrue((Boolean) method.invoke(null, "nướng"));
            assertTrue((Boolean) method.invoke(null, "ướp"));
            assertTrue((Boolean) method.invoke(null, "VIỆT"));
            assertTrue((Boolean) method.invoke(null, "Đà Nẵng"));
        }

        @Test
        void withoutVietnameseAccents_shouldReturnFalse() throws Exception {
            var method =
                    RecipeSpecification.class.getDeclaredMethod("hasVietnameseAccent", String.class);
            method.setAccessible(true);

            assertFalse((Boolean) method.invoke(null, "pho"));
            assertFalse((Boolean) method.invoke(null, "dang"));
            assertFalse((Boolean) method.invoke(null, "com"));
            assertFalse((Boolean) method.invoke(null, "hello"));
            assertFalse((Boolean) method.invoke(null, "world"));
            assertFalse((Boolean) method.invoke(null, ""));
            assertFalse((Boolean) method.invoke(null, "abc123"));
            assertFalse((Boolean) method.invoke(null, "test123"));
        }
    }
}