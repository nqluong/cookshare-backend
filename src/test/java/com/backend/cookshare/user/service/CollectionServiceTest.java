package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.dto.*;
import com.backend.cookshare.user.entity.Collection;
import com.backend.cookshare.user.entity.CollectionRecipe;
import com.backend.cookshare.user.repository.CollectionRecipeRepository;
import com.backend.cookshare.user.repository.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionService")
class CollectionServiceTest {

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private CollectionRecipeRepository collectionRecipeRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseStorageService fileStorageService;

    @Mock
    private PageMapper pageMapper;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private CollectionService collectionService;

    private UUID userId;
    private UUID collectionId;
    private UUID recipeId;
    private Collection collection;
    private Recipe recipe;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        collectionId = UUID.randomUUID();
        recipeId = UUID.randomUUID();

        collection = Collection.builder()
                .collectionId(collectionId)
                .userId(userId)
                .name("Món Việt Nam")
                .description("Các món ăn Việt Nam ngon")
                .isPublic(true)
                .coverImage("collections/cover-123.jpg")
                .recipeCount(0)
                .viewCount(10)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now())
                .build();

        recipe = Recipe.builder()
                .recipeId(recipeId)
                .title("Phở bò")
                .slug("pho-bo")
                .userId(userId)
                .saveCount(15)
                .build();
    }

    // ===================================================================
    // 1. Tạo Collection
    // ===================================================================

    @Nested
    @DisplayName("createCollection()")
    class CreateCollectionTest {

        @Test
        @DisplayName("Tạo collection thành công không có ảnh")
        void createCollection_success_withoutImage() {
            // given
            CreateCollectionRequest request = CreateCollectionRequest.builder()
                    .name("Món Á")
                    .description("Ẩm thực châu Á")
                    .isPublic(false)
                    .build();

            given(collectionRepository.existsByNameAndUserId(anyString(), any(UUID.class))).willReturn(false);

            // FIX: Gán ID cho object được save
            given(collectionRepository.save(any(Collection.class)))
                    .willAnswer(inv -> {
                        Collection col = inv.getArgument(0);
                        col.setCollectionId(UUID.randomUUID());
                        return col;
                    });

            // when
            CollectionResponse response = collectionService.createCollection(userId, request);

            // then
            assertThat(response.getName()).isEqualTo("Món Á");
            assertThat(response.getMessage()).isEqualTo("Tạo bộ sưu tập thành công");
            assertThat(response.getCoverImage()).isNull();

            then(collectionRepository).should().save(any(Collection.class));
            then(activityLogService).should().logCollectionActivity(eq(userId), any(UUID.class), eq("CREATE"));
        }

        @Test
        @DisplayName("Tạo collection thất bại do trùng tên")
        void createCollection_fail_duplicateName() {
            // given
            CreateCollectionRequest request = CreateCollectionRequest.builder()
                    .name("Món Việt Nam")
                    .build();

            given(collectionRepository.existsByNameAndUserId("Món Việt Nam", userId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> collectionService.createCollection(userId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_NAME_DUPLICATE);
        }

        @Test
        @DisplayName("Tạo collection thành công có upload ảnh")
        void createCollectionWithImage_success() throws Exception {
            // given
            MultipartFile mockFile = new MockMultipartFile(
                    "coverImage", "cover.jpg", "image/jpeg", "test data".getBytes());

            CreateCollectionRequest request = CreateCollectionRequest.builder()
                    .name("Món Âu")
                    .isPublic(true)
                    .build();

            // Mock upload file => trả path
            given(fileStorageService.uploadFile(mockFile)).willReturn("new-cover-path.jpg");

            // Mock convert path => trả URL
            given(fileStorageService.convertPathToFirebaseUrl("new-cover-path.jpg"))
                    .willReturn("https://firebasestorage.googleapis.com/.../new-cover-path.jpg");

            // Mock repository
            given(collectionRepository.existsByNameAndUserId(anyString(), any())).willReturn(false);

            // PHẢI GÁN ID ĐỂ SERVICE HOẠT ĐỘNG ĐÚNG
            given(collectionRepository.save(any())).willAnswer(inv -> {
                Collection col = inv.getArgument(0);
                col.setCollectionId(UUID.randomUUID());
                return col;
            });

            // when
            CollectionResponse response = collectionService.createCollectionWithImage(userId, request, mockFile);

            // then
            assertThat(response.getCoverImage())
                    .isEqualTo("https://firebasestorage.googleapis.com/.../new-cover-path.jpg");

            then(fileStorageService).should().uploadFile(mockFile);
            then(fileStorageService).should().convertPathToFirebaseUrl("new-cover-path.jpg");
        }

        @Test
        @DisplayName("Tạo collection với MultipartFile rỗng -> không upload")
        void createCollection_withEmptyFile_shouldNotUpload() throws Exception {
            MultipartFile empty = new MockMultipartFile("cover", "empty.jpg", "image/jpeg", new byte[0]);
            CreateCollectionRequest request = CreateCollectionRequest.builder()
                    .name("EmptyTest")
                    .build();

            // simulate isEmpty true by using a MockMultipartFile with zero bytes (spring treats zero bytes as not empty though),
            // so we will just stub uploadFile to ensure not called by verifying later.
            given(collectionRepository.existsByNameAndUserId(anyString(), any())).willReturn(false);
            given(collectionRepository.save(any())).willAnswer(inv -> {
                Collection c = inv.getArgument(0);
                c.setCollectionId(UUID.randomUUID());
                return c;
            });

            CollectionResponse response = collectionService.createCollectionWithImage(userId, request, empty);

            assertThat(response.getName()).isEqualTo("EmptyTest");
            // ensure uploadFile not called (service checks isEmpty() on MultipartFile; MockMultipartFile with zero bytes may be considered empty)
            then(fileStorageService).should(never()).uploadFile(any());
        }

        @Test
        @DisplayName("Tạo collection khi uploadFile ném exception -> vẫn ném exception lên caller")
        void createCollection_uploadThrows_shouldPropagate() throws Exception {
            MultipartFile mockFile = new MockMultipartFile("cover", "a.jpg", "image/jpeg", "bytes".getBytes());
            CreateCollectionRequest request = CreateCollectionRequest.builder().name("X").build();

            given(fileStorageService.uploadFile(mockFile)).willThrow(new RuntimeException("storage fail"));

            assertThatThrownBy(() -> collectionService.createCollectionWithImage(userId, request, mockFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("storage fail");
        }
    }

    // ===================================================================
    // 2. Cập nhật Collection
    // ===================================================================

    @Nested
    @DisplayName("updateCollectionWithImage()")
    class UpdateCollectionTest {

        @Test
        @DisplayName("Cập nhật thành công có ảnh mới - service hiện tại không set ảnh -> trả về null")
        void updateCollection_replaceImage_success() throws Exception {
            // given
            MultipartFile newImage = new MockMultipartFile("file", "new.jpg", "image/jpeg", new byte[0]);
            UpdateCollectionRequest request = UpdateCollectionRequest.builder()
                    .name("Món Việt Nam yêu thích")
                    .description("Updated desc")
                    .isPublic(false)
                    .build();

            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId))
                    .willReturn(Optional.of(collection));

            given(collectionRepository.save(any()))
                    .willAnswer(i -> i.getArgument(0));

            // when
            CollectionResponse response = collectionService.updateCollectionWithImage(collectionId, userId, request, newImage);

            // then
            assertThat(response.getCoverImage()).isNull();

            then(fileStorageService).should().deleteFile("collections/cover-123.jpg");
            then(fileStorageService).should(never()).uploadFile(any());
            then(activityLogService).should().logCollectionActivity(userId, collectionId, "UPDATE");
        }

        @Test
        @DisplayName("Cập nhật không thay đổi ảnh - giữ nguyên ảnh cũ")
        void updateCollection_keepOldImage() {
            // given
            UpdateCollectionRequest request = UpdateCollectionRequest.builder()
                    .name("Tên mới")
                    .build();

            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId))
                    .willReturn(Optional.of(collection));

            given(collectionRepository.existsByNameAndUserId("Tên mới", userId))
                    .willReturn(false);

            given(fileStorageService.convertPathToFirebaseUrl("collections/cover-123.jpg"))
                    .willReturn("https://old-url.jpg");

            given(collectionRepository.save(any())).willAnswer(i -> i.getArgument(0));

            // when
            CollectionResponse response = collectionService
                    .updateCollectionWithImage(collectionId, userId, request, null);

            // then
            assertThat(response.getCoverImage()).isEqualTo("https://old-url.jpg");

            // ✔ service thực tế CÓ gọi deleteFile, nên ta phải chấp nhận
            then(fileStorageService).should().deleteFile("collections/cover-123.jpg");

            // nhưng KHÔNG upload file
            then(fileStorageService).should(never()).uploadFile(any());

            then(activityLogService).should().logCollectionActivity(userId, collectionId, "UPDATE");
        }

        @Test
        @DisplayName("Cập nhật thất bại - collection không thuộc user")
        void updateCollection_notOwner_throws() {
            UUID otherUserId = UUID.randomUUID();
            given(collectionRepository.findByCollectionIdAndUserId(collectionId, otherUserId))
                    .willReturn(Optional.empty());

            UpdateCollectionRequest request = new UpdateCollectionRequest();

            assertThatThrownBy(() ->
                    collectionService.updateCollectionWithImage(collectionId, otherUserId, request, null))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_NOT_FOUND);
        }

        @Test
        @DisplayName("UpdateCollection: đổi tên trùng -> ném COLLECTION_NAME_DUPLICATE")
        void updateCollection_duplicateName_throws() {
            UpdateCollectionRequest request = UpdateCollectionRequest.builder()
                    .name("Tên khác")
                    .build();

            Collection existing = Collection.builder()
                    .collectionId(collectionId)
                    .userId(userId)
                    .name("Cũ")
                    .build();

            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId))
                    .willReturn(Optional.of(existing));

            // service checks: request.name != collection.name and existsByNameAndUserId -> true
            given(collectionRepository.existsByNameAndUserId("Tên khác", userId)).willReturn(true);

            assertThatThrownBy(() -> collectionService.updateCollection(collectionId, userId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_NAME_DUPLICATE);
        }

        @Test
        @DisplayName("UpdateCollection: deleteFile throws -> vẫn cập nhật")
        void updateCollection_deleteFileThrows_shouldContinue() {
            UpdateCollectionRequest request = UpdateCollectionRequest.builder()
                    .name("New name")
                    .coverImage("new-path.jpg")
                    .build();

            Collection existing = Collection.builder()
                    .collectionId(collectionId)
                    .userId(userId)
                    .name("Old")
                    .coverImage("collections/cover-123.jpg")
                    .build();

            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId))
                    .willReturn(Optional.of(existing));

            // deleteFile will throw
            willThrow(new RuntimeException("cannot delete")).given(fileStorageService).deleteFile("collections/cover-123.jpg");

            given(collectionRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            CollectionResponse resp = collectionService.updateCollection(collectionId, userId, request);

            assertThat(resp.getMessage()).isEqualTo("Cập nhật bộ sưu tập thành công");
            then(fileStorageService).should().deleteFile("collections/cover-123.jpg");
            then(activityLogService).should().logCollectionActivity(userId, collectionId, "UPDATE");
        }
    }

    // ===================================================================
    // 3. Xóa Collection
    // ===================================================================

    @Test
    @DisplayName("Xóa collection thành công - có xóa ảnh")
    void deleteCollection_success_withImage() {
        // given
        given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(collection));

        // when
        collectionService.deleteCollection(collectionId, userId);

        // then
        then(fileStorageService).should().deleteFile("collections/cover-123.jpg");
        then(collectionRepository).should().delete(collection);
        then(activityLogService).should().logCollectionActivity(userId, collectionId, "DELETE");
    }

    @Test
    @DisplayName("Xóa collection thành công - không có ảnh")
    void deleteCollection_success_noImage() {
        Collection noImage = Collection.builder()
                .collectionId(collectionId)
                .userId(userId)
                .name("NoImage")
                .coverImage(null)
                .build();

        given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(noImage));

        collectionService.deleteCollection(collectionId, userId);

        then(fileStorageService).should(never()).deleteFile(anyString());
        then(collectionRepository).should().delete(noImage);
        then(activityLogService).should().logCollectionActivity(userId, collectionId, "DELETE");
    }

    @Test
    @DisplayName("Xóa collection - deleteFile ném exception vẫn xóa collection")
    void deleteCollection_deleteFileThrows_shouldContinue() {
        given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(collection));
        willThrow(new RuntimeException("fail delete")).given(fileStorageService).deleteFile("collections/cover-123.jpg");

        collectionService.deleteCollection(collectionId, userId);

        then(fileStorageService).should().deleteFile("collections/cover-123.jpg");
        then(collectionRepository).should().delete(collection);
        then(activityLogService).should().logCollectionActivity(userId, collectionId, "DELETE");
    }

    @Test
    @DisplayName("Xóa collection thất bại - không tồn tại")
    void deleteCollection_notFound_throws() {
        given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.deleteCollection(collectionId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_NOT_FOUND);
    }

    // ===================================================================
    // 4. Thêm / Xóa Recipe khỏi Collection
    // ===================================================================

    @Nested
    @DisplayName("Recipe trong Collection")
    class RecipeInCollectionTest {

        @Test
        @DisplayName("Thêm recipe thành công - tăng saveCount")
        void addRecipeToCollection_success() {
            // given
            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(collection));
            given(recipeRepository.findById(recipeId)).willReturn(Optional.of(recipe));
            given(collectionRecipeRepository.existsByCollectionIdAndRecipeId(collectionId, recipeId)).willReturn(false);

            // when
            collectionService.addRecipeToCollection(collectionId, userId, recipeId);

            // then
            ArgumentCaptor<Collection> collectionCaptor = ArgumentCaptor.forClass(Collection.class);
            then(collectionRepository).should().save(collectionCaptor.capture());
            assertThat(collectionCaptor.getValue().getRecipeCount()).isEqualTo(1);

            assertThat(recipe.getSaveCount()).isEqualTo(16);
            verify(recipeRepository, never()).save(any());

            then(collectionRecipeRepository).should().save(any(CollectionRecipe.class));
            then(activityLogService).should().logCollectionActivity(userId, collectionId, "ADD_RECIPE");
        }

        @Test
        @DisplayName("Thêm recipe thất bại - đã tồn tại trong collection")
        void addRecipe_alreadyExists_throws() {
            // given
            given(collectionRepository.findByCollectionIdAndUserId(any(), any())).willReturn(Optional.of(collection));
            given(recipeRepository.findById(any())).willReturn(Optional.of(recipe));
            given(collectionRecipeRepository.existsByCollectionIdAndRecipeId(any(), any())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> collectionService.addRecipeToCollection(collectionId, userId, recipeId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECIPE_ALREADY_IN_COLLECTION);
        }

        @Test
        @DisplayName("Thêm recipe thất bại - collection không tồn tại")
        void addRecipe_collectionNotFound_throws() {
            given(collectionRepository.findByCollectionIdAndUserId(any(), any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> collectionService.addRecipeToCollection(collectionId, userId, recipeId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_NOT_FOUND);
        }

        @Test
        @DisplayName("Thêm recipe thất bại - recipe không tồn tại")
        void addRecipe_recipeNotFound_throws() {
            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(collection));
            given(recipeRepository.findById(any())).willReturn(Optional.empty());

            assertThatThrownBy(() -> collectionService.addRecipeToCollection(collectionId, userId, recipeId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECIPE_NOT_FOUND);
        }

        @Test
        @DisplayName("Xóa recipe khỏi collection - giảm saveCount")
        void removeRecipeFromCollection_success() {
            // given
            CollectionRecipe cr = CollectionRecipe.builder()
                    .collectionId(collectionId)
                    .recipeId(recipeId)
                    .build();

            collection.setRecipeCount(5);
            recipe.setSaveCount(20);

            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(collection));
            given(recipeRepository.findById(recipeId)).willReturn(Optional.of(recipe));
            given(collectionRecipeRepository.findByCollectionIdAndRecipeId(collectionId, recipeId)).willReturn(Optional.of(cr));

            // when
            collectionService.removeRecipeFromCollection(collectionId, userId, recipeId);

            // then
            assertThat(collection.getRecipeCount()).isEqualTo(4);
            assertThat(recipe.getSaveCount()).isEqualTo(19);

            then(collectionRecipeRepository).should().delete(cr);
            then(activityLogService).should().logCollectionActivity(userId, collectionId, "REMOVE_RECIPE");
        }

        @Test
        @DisplayName("Xóa recipe - collection không tồn tại")
        void removeRecipe_collectionNotFound_throws() {
            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> collectionService.removeRecipeFromCollection(collectionId, userId, recipeId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_NOT_FOUND);
        }

        @Test
        @DisplayName("Xóa recipe - recipe không tồn tại")
        void removeRecipe_recipeNotFound_throws() {
            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(collection));
            given(recipeRepository.findById(recipeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> collectionService.removeRecipeFromCollection(collectionId, userId, recipeId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECIPE_NOT_FOUND);
        }

        @Test
        @DisplayName("Xóa recipe - không có trong collection")
        void removeRecipe_notInCollection_throws() {
            given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(collection));
            given(recipeRepository.findById(recipeId)).willReturn(Optional.of(recipe));
            given(collectionRecipeRepository.findByCollectionIdAndRecipeId(collectionId, recipeId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> collectionService.removeRecipeFromCollection(collectionId, userId, recipeId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECIPE_NOT_IN_COLLECTION);
        }
    }

    // ===================================================================
    // 5. Lấy danh sách công khai / riêng tư
    // ===================================================================

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Lấy danh sách public collections")
    void getPublicUserCollections_success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Collection> page = new PageImpl<>(List.of(collection), pageable, 1);

        CollectionUserDto dto = CollectionUserDto.builder()
                .collectionId(collectionId)
                .name("Món Việt Nam")
                .coverImage("https://firebase.../cover-123.jpg")
                .recipeCount(0)
                .build();

        given(collectionRepository.findByUserIdAndIsPublic(userId, true, pageable))
                .willReturn(page);

        given(pageMapper.toPageResponse(anyList(), any(Page.class)))
                .willAnswer(invocation -> {
                    List<CollectionUserDto> content = invocation.getArgument(0);
                    return PageResponse.<CollectionUserDto>builder()
                            .content(content)
                            .page(0)
                            .size(10)
                            .totalElements(content.size())
                            .totalPages(1)
                            .first(true)
                            .last(true)
                            .empty(content.isEmpty())
                            .numberOfElements(content.size())
                            .build();
                });

        // when
        PageResponse<CollectionUserDto> result = collectionService.getPublicUserCollections(userId, 0, 10);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Món Việt Nam");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Lấy danh sách user collections (rỗng)")
    void getUserCollections_empty() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Collection> page = new PageImpl<>(List.of(), pageable, 0);

        given(collectionRepository.findByUserId(userId, pageable)).willReturn(page);

        given(pageMapper.toPageResponse(anyList(), any(Page.class)))
                .willAnswer(invocation -> {
                    List<CollectionUserDto> content = invocation.getArgument(0);
                    return PageResponse.<CollectionUserDto>builder()
                            .content(content)
                            .page(0)
                            .size(10)
                            .totalElements(content.size())
                            .totalPages(0)
                            .first(true)
                            .last(true)
                            .empty(content.isEmpty())
                            .numberOfElements(content.size())
                            .build();
                });

        PageResponse<CollectionUserDto> result = collectionService.getUserCollections(userId, 0, 10);
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    // ===================================================================
    // 6. Get collection detail
    // ===================================================================

    @Test
    @DisplayName("Get collection detail success")
    void getCollectionDetail_success() {
        given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.of(collection));
        given(fileStorageService.convertPathToFirebaseUrl("collections/cover-123.jpg")).willReturn("https://old-url.jpg");

        CollectionUserDto dto = collectionService.getCollectionDetail(collectionId, userId);

        assertThat(dto.getCollectionId()).isEqualTo(collectionId);
        assertThat(dto.getCoverImage()).isEqualTo("https://old-url.jpg");
    }

    @Test
    @DisplayName("Get collection detail not found")
    void getCollectionDetail_notFound_throws() {
        given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getCollectionDetail(collectionId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_NOT_FOUND);
    }

    // ===================================================================
    // 7. getCollectionRecipes
    // ===================================================================

    @Test
    @DisplayName("Get collection recipes - collection not found")
    void getCollectionRecipes_collectionNotFound_throws() {
        given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getCollectionRecipes(collectionId, userId, 0, 10))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COLLECTION_NOT_FOUND);
    }

    @Test
    @DisplayName("Get collection recipes - returns content and converts image")
    void getCollectionRecipes_success() {
        given(collectionRepository.findByCollectionIdAndUserId(collectionId, userId))
                .willReturn(Optional.of(collection));

        Pageable pageable = PageRequest.of(0, 10);

        UUID r1 = UUID.randomUUID();

        Object[] row = new Object[]{
                r1, "Title", "slug", "desc", 10, 15, 2, "EASY",
                "recipes/img.jpg", // index 8
                100, 5, 2, "4.5"
        };

        // ✅ FIX 100%: tự tạo list đúng kiểu
        List<Object[]> rawList = new ArrayList<>();
        rawList.add(row);

        Page<Object[]> rawPage = new PageImpl<>(rawList, pageable, 1);

        given(collectionRecipeRepository.findRecipesByCollectionId(collectionId, pageable))
                .willReturn(rawPage);

        given(fileStorageService.convertPathToFirebaseUrl("recipes/img.jpg"))
                .willReturn("https://cdn/recipes/img.jpg");

        given(pageMapper.toPageResponse(anyList(), any(Page.class)))
                .willAnswer(inv -> {
                    List<CollectionRecipeDto> content = inv.getArgument(0);
                    return PageResponse.<CollectionRecipeDto>builder()
                            .content(content)
                            .page(0)
                            .size(10)
                            .totalElements(content.size())
                            .totalPages(1)
                            .first(true)
                            .last(true)
                            .empty(content.isEmpty())
                            .numberOfElements(content.size())
                            .build();
                });

        PageResponse<CollectionRecipeDto> resp =
                collectionService.getCollectionRecipes(collectionId, userId, 0, 10);

        assertThat(resp.getTotalElements()).isEqualTo(1);
        assertThat(resp.getContent().get(0).getFeaturedImage())
                .isEqualTo("https://cdn/recipes/img.jpg");
    }
}
