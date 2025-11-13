package com.backend.cookshare.user.controller;

import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.user.dto.*;
import com.backend.cookshare.user.service.CollectionService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class CollectionController {

    private final CollectionService collectionService;
    private final Path root = Paths.get(System.getProperty("user.dir"), "uploads", "recipe_images");

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                log.info("Created directory for recipe images: {}", root.toAbsolutePath());
            } else {
                log.info("Directory already exists: {}", root.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not create upload directory! Path: {}", root.toAbsolutePath(), e);
            log.warn("Upload functionality may not work properly. Please check directory permissions.");
            // Không throw exception để ứng dụng vẫn có thể khởi động
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File rỗng"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File phải là ảnh"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File không được vượt quá 5MB"));
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID() + fileExtension;
            Path filePath = root.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            String url = "recipe_images/" + fileName;
            log.info("File uploaded successfully: {}", url);

            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(500).body(Map.of("error", "Lỗi khi upload ảnh: " + e.getMessage()));
        }
    }

    // Tạo bộ sưu tập mới
    @PostMapping("/{userId}/collections")
    public ResponseEntity<ApiResponse<CollectionResponse>> createCollection(
            @PathVariable UUID userId,
            @RequestBody @Valid CreateCollectionRequest request) {

        log.info("POST /users/{}/collections - Request: {}", userId, request);

        CollectionResponse response = collectionService.createCollection(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CollectionResponse>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("Tạo bộ sưu tập thành công")
                        .data(response)
                        .build());
    }

    // Lấy danh sách bộ sưu tập của user
    @GetMapping("/{userId}/collections")
    public ResponseEntity<ApiResponse<PageResponse<CollectionUserDto>>> getUserCollections(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /users/{}/collections?page={}&size={}", userId, page, size);

        PageResponse<CollectionUserDto> collections = collectionService.getUserCollections(userId, page, size);

        return ResponseEntity.ok(ApiResponse.<PageResponse<CollectionUserDto>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách bộ sưu tập thành công")
                .data(collections)
                .build());
    }

    // Lấy danh sách public bộ sưu tập của user
    @GetMapping("/{userId}/collections/public")
    public ResponseEntity<ApiResponse<PageResponse<CollectionUserDto>>> getPublicCollections(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /users/{}/collections/public?page={}&size={}", userId, page, size);

        PageResponse<CollectionUserDto> collections = collectionService.getPublicUserCollections(userId, page, size);

        return ResponseEntity.ok(ApiResponse.<PageResponse<CollectionUserDto>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách bộ sưu tập công khai thành công")
                .data(collections)
                .build());
    }

    // Lấy chi tiết bộ sưu tập
    @GetMapping("/{userId}/collections/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionUserDto>> getCollectionDetail(
            @PathVariable UUID userId,
            @PathVariable UUID collectionId) {

        log.info("GET /users/{}/collections/{}", userId, collectionId);

        CollectionUserDto collection = collectionService.getCollectionDetail(collectionId, userId);

        return ResponseEntity.ok(ApiResponse.<CollectionUserDto>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy chi tiết bộ sưu tập thành công")
                .data(collection)
                .build());
    }

    // Cập nhật bộ sưu tập
    @PutMapping("/{userId}/collections/{collectionId}")
    public ResponseEntity<ApiResponse<CollectionResponse>> updateCollection(
            @PathVariable UUID userId,
            @PathVariable UUID collectionId,
            @RequestBody @Valid UpdateCollectionRequest request) {

        log.info("PUT /users/{}/collections/{} - Request: {}", userId, collectionId, request);

        CollectionResponse response = collectionService.updateCollection(collectionId, userId, request);

        return ResponseEntity.ok(ApiResponse.<CollectionResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Cập nhật bộ sưu tập thành công")
                .data(response)
                .build());
    }

    // Xóa bộ sưu tập
    @DeleteMapping("/{userId}/collections/{collectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteCollection(
            @PathVariable UUID userId,
            @PathVariable UUID collectionId) {

        log.info("DELETE /users/{}/collections/{}", userId, collectionId);

        collectionService.deleteCollection(collectionId, userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa bộ sưu tập thành công")
                .build());
    }

    @GetMapping("/{userId}/collections/{collectionId}/recipes")
    public ResponseEntity<ApiResponse<PageResponse<CollectionRecipeDto>>> getCollectionRecipes(
            @PathVariable UUID userId,
            @PathVariable UUID collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /users/{}/collections/{}/recipes?page={}&size={}", userId, collectionId, page, size);

        PageResponse<CollectionRecipeDto> recipes = collectionService.getCollectionRecipes(
                collectionId, userId, page, size);

        return ResponseEntity.ok(ApiResponse.<PageResponse<CollectionRecipeDto>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy danh sách công thức trong bộ sưu tập thành công")
                .data(recipes)
                .build());
    }

    // Thêm recipe vào collection
    @PostMapping("/{userId}/collections/{collectionId}/recipes")
    public ResponseEntity<ApiResponse<Void>> addRecipeToCollection(
            @PathVariable UUID userId,
            @PathVariable UUID collectionId,
            @RequestBody @Valid AddRecipeToCollectionRequest request) {

        log.info("POST /users/{}/collections/{}/recipes - Request: {}", userId, collectionId, request);

        collectionService.addRecipeToCollection(collectionId, userId, request.getRecipeId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                        .code(HttpStatus.CREATED.value())
                        .message("Thêm công thức vào bộ sưu tập thành công")
                        .build());
    }

    // Xóa recipe khỏi collection
    @DeleteMapping("/{userId}/collections/{collectionId}/recipes/{recipeId}")
    public ResponseEntity<ApiResponse<Void>> removeRecipeFromCollection(
            @PathVariable UUID userId,
            @PathVariable UUID collectionId,
            @PathVariable UUID recipeId) {

        log.info("DELETE /users/{}/collections/{}/recipes/{}", userId, collectionId, recipeId);

        collectionService.removeRecipeFromCollection(collectionId, userId, recipeId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Xóa công thức khỏi bộ sưu tập thành công")
                .build());
    }
}