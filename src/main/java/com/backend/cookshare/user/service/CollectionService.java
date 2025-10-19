package com.backend.cookshare.user.service;

import com.backend.cookshare.authentication.entity.User;
import com.backend.cookshare.authentication.repository.UserRepository;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.common.mapper.PageMapper;
import com.backend.cookshare.recipe_management.dto.RecipeResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.repository.RecipeRepository;
import com.backend.cookshare.user.dto.*;
import com.backend.cookshare.user.entity.Collection;
import com.backend.cookshare.user.entity.CollectionRecipe;
import com.backend.cookshare.user.entity.CollectionRecipeId;
import com.backend.cookshare.user.repository.CollectionRecipeRepository;
import com.backend.cookshare.user.repository.CollectionRepository;
import com.backend.cookshare.user.websocket.WebSocketNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionRecipeRepository collectionRecipeRepository;
    private final PageMapper pageMapper;
    private final WebSocketNotificationSender webSocketNotificationSender;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    // Tạo collection mới
    @Transactional
    public CollectionResponse createCollection(UUID userId, CreateCollectionRequest request) {
        log.info("Creating collection for user: {}", userId);

        // Kiểm tra tên collection đã tồn tại chưa
        if (collectionRepository.existsByNameAndUserId(request.getName(), userId)) {
            throw new CustomException(
                    ErrorCode.COLLECTION_NAME_DUPLICATE
            );
        }

        Collection collection = Collection.builder()
                .userId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .isPublic(request.getIsPublic())
                .coverImage(request.getCoverImage())
                .recipeCount(0)
                .viewCount(0)
                .build();

        Collection saved = collectionRepository.save(collection);
        log.info("Collection created successfully: {}", saved.getCollectionId());

        return mapToResponse(saved, "Tạo bộ sưu tập thành công");
    }

    // Lấy danh sách collections của user
    @Transactional(readOnly = true)
    public PageResponse<CollectionUserDto> getUserCollections(UUID userId, int page, int size) {
        log.info("Getting collections for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Collection> collections = collectionRepository.findByUserId(userId, pageable);

        List<CollectionUserDto> content = collections.getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(content, collections);
    }

    // Lấy danh sách public collections của user
    @Transactional(readOnly = true)
    public PageResponse<CollectionUserDto> getPublicUserCollections(UUID userId, int page, int size) {
        log.info("Getting public collections for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<Collection> collections = collectionRepository.findByUserIdAndIsPublic(userId, true, pageable);

        List<CollectionUserDto> content = collections.getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return pageMapper.toPageResponse(content, collections);
    }

    // Lấy chi tiết collection
    @Transactional(readOnly = true)
    public CollectionUserDto getCollectionDetail(UUID collectionId, UUID userId) {
        log.info("Getting collection detail: {}", collectionId);

        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND));

        return mapToDto(collection);
    }

    // Cập nhật collection
    @Transactional
    public CollectionResponse updateCollection(UUID collectionId, UUID userId, UpdateCollectionRequest request) {
        log.info("Updating collection: {}", collectionId);

        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND));

        // Kiểm tra nếu đổi tên, tên mới không được trùng
        if (request.getName() != null &&
                !request.getName().equals(collection.getName()) &&
                collectionRepository.existsByNameAndUserId(request.getName(), userId)) {
            throw new CustomException(
                    ErrorCode.COLLECTION_NAME_DUPLICATE
            );
        }

        if (request.getName() != null) {
            collection.setName(request.getName());
        }
        if (request.getDescription() != null) {
            collection.setDescription(request.getDescription());
        }
        if (request.getIsPublic() != null) {
            collection.setIsPublic(request.getIsPublic());
        }
        if (request.getCoverImage() != null) {
            collection.setCoverImage(request.getCoverImage());
        }

        Collection updated = collectionRepository.save(collection);
        log.info("Collection updated successfully");

        return mapToResponse(updated, "Cập nhật bộ sưu tập thành công");
    }

    // Xóa collection
    @Transactional
    public void deleteCollection(UUID collectionId, UUID userId) {
        log.info("Deleting collection: {}", collectionId);

        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND));

        collectionRepository.delete(collection);
        log.info("Collection deleted successfully");
    }

    // Lấy danh sách recipes của collection
    @Transactional(readOnly = true)
    public PageResponse<CollectionRecipeDto> getCollectionRecipes(
            UUID collectionId, UUID userId, int page, int size) {
        log.info("Getting recipes for collection: {}", collectionId);

        // Kiểm tra collection tồn tại
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> rawRecipes = collectionRecipeRepository.findRecipesByCollectionId(
                collectionId, pageable);

        List<CollectionRecipeDto> content = rawRecipes.getContent()
                .stream()
                .map(row -> mapToCollectionRecipeDto(row))
                .collect(Collectors.toList());

        log.info("Get collection recipes successful, count: {}", content.size());

        return pageMapper.toPageResponse(content, rawRecipes);
    }

    // Thêm recipe vào collection
    @Transactional
    public void addRecipeToCollection(UUID collectionId, UUID userId, UUID recipeId) {
        log.info("Adding recipe {} to collection {}", recipeId, collectionId);

        // Kiểm tra collection tồn tại
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND));

        // Kiểm tra recipe tồn tại
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        // Kiểm tra recipe đã có trong collection chưa
        if (collectionRecipeRepository.existsByCollectionIdAndRecipeId(collectionId, recipeId)) {
            throw new CustomException(
                    ErrorCode.RECIPE_ALREADY_IN_COLLECTION
            );
        }

        // Thêm recipe vào collection
        CollectionRecipe collectionRecipe = CollectionRecipe.builder()
                .collectionId(collectionId)
                .recipeId(recipeId)
                .build();

        collectionRecipeRepository.save(collectionRecipe);

        // 5️⃣ Gửi thông báo WebSocket tới chủ sở hữu công thức (nếu khác user đang thao tác)

        if (!recipe.getUserId().equals(userId)) {
            User sharer = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            webSocketNotificationSender.sendShareNotification(
                    recipe.getUserId(),          // Chủ công thức nhận thông báo
                    recipeId,                    // ID công thức
                    sharer.getUsername(),        // Tên người chia sẻ
                    collection.getName(),        // Tên bộ sưu tập
                    recipe.getTitle()            // Tên công thức
            );
        }
        // Cập nhật recipe count
        collection.setRecipeCount(collection.getRecipeCount() + 1);
        collectionRepository.save(collection);

        log.info("Recipe added to collection successfully");
    }

    // Xóa recipe khỏi collection
    @Transactional
    public void removeRecipeFromCollection(UUID collectionId, UUID userId, UUID recipeId) {
        log.info("Removing recipe {} from collection {}", recipeId, collectionId);

        // Kiểm tra collection tồn tại
        Collection collection = collectionRepository.findByCollectionIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND));

        // Kiểm tra recipe có trong collection
        CollectionRecipe collectionRecipe = collectionRecipeRepository
                .findByCollectionIdAndRecipeId(collectionId, recipeId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.RECIPE_NOT_IN_COLLECTION
                ));

        // Xóa recipe khỏi collection
        collectionRecipeRepository.delete(collectionRecipe);

        // Cập nhật recipe count
        collection.setRecipeCount(Math.max(0, collection.getRecipeCount() - 1));
        collectionRepository.save(collection);

        log.info("Recipe removed from collection successfully");
    }

    // Helper methods
    private CollectionUserDto mapToDto(Collection collection) {
        return CollectionUserDto.builder()
                .collectionId(collection.getCollectionId())
                .userId(collection.getUserId())
                .name(collection.getName())
                .description(collection.getDescription())
                .isPublic(collection.getIsPublic())
                .coverImage(collection.getCoverImage())
                .recipeCount(collection.getRecipeCount())
                .viewCount(collection.getViewCount())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .build();
    }

    private CollectionResponse mapToResponse(Collection collection, String message) {
        return CollectionResponse.builder()
                .collectionId(collection.getCollectionId())
                .name(collection.getName())
                .description(collection.getDescription())
                .isPublic(collection.getIsPublic())
                .coverImage(collection.getCoverImage())
                .recipeCount(collection.getRecipeCount())
                .viewCount(collection.getViewCount())
                .createdAt(collection.getCreatedAt())
                .updatedAt(collection.getUpdatedAt())
                .message(message)
                .build();
    }

    // Map từ Object[] sang DTO
    private CollectionRecipeDto mapToCollectionRecipeDto(Object[] row) {
        return CollectionRecipeDto.builder()
                .recipeId((UUID) row[0])
                .title((String) row[1])
                .slug((String) row[2])
                .description((String) row[3])
                .prepTime((Integer) row[4])
                .cookTime((Integer) row[5])
                .servings((Integer) row[6])
                .difficulty(((Object) row[7]).toString())
                .featuredImage((String) row[8])
                .viewCount((Integer) row[9])
                .saveCount((Integer) row[10])
                .likeCount((Integer) row[11])
                .averageRating((String) row[12])
                .build();
    }
}