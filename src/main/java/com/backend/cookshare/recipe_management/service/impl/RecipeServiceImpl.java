package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeDetailsResult;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.mapper.RecipeMapper;
import com.backend.cookshare.recipe_management.repository.*;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.recipe_management.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeTagRepository recipeTagRepository;
    private final RecipeCategoryRepository recipeCategoryRepository;
    private final RecipeMapper recipeMapper;
    private final RecipeLoaderHelper recipeLoaderHelper;
    private final FirebaseStorageService fileStorageService;

    // ================= CREATE =================

    @Override
    @Transactional
    public RecipeResponse createRecipeWithFiles(RecipeRequest request, MultipartFile image, List<MultipartFile> stepImages) {
        // Upload ảnh đại diện nếu có
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.uploadFile(image);
            request.setFeaturedImage(imageUrl);
        }

        // Upload và map ảnh cho từng bước theo stepNumber
        if (request.getSteps() != null && stepImages != null && !stepImages.isEmpty()) {
            // Tạo map từ stepNumber -> imageUrl
            Map<Integer, String> stepImageMap = mapStepImages(stepImages);

            // Gán ảnh cho đúng step dựa trên stepNumber
            request.getSteps().forEach(step -> {
                Integer stepNumber = step.getStepNumber();
                if (stepNumber != null && stepImageMap.containsKey(stepNumber)) {
                    step.setImageUrl(stepImageMap.get(stepNumber));
                    log.info("Mapped image to step {}: {}", stepNumber, stepImageMap.get(stepNumber));
                }
            });
        }

        return createRecipe(request);
    }

    @Override
    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request) {
        Recipe recipe = recipeMapper.toEntity(request);

        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        Recipe savedRecipe = recipeRepository.save(recipe);
        UUID recipeId = savedRecipe.getRecipeId();

        saveRecipeRelations(recipeId, request);
        return loadRecipeResponse(savedRecipe);
    }

    // ================= UPDATE =================

    @Override
    @Transactional
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request,
                                       MultipartFile image, List<MultipartFile> stepImages) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy recipe id: " + id));

        log.info("🔄 Bắt đầu cập nhật recipe: {}", id);

        // ========== LẤY DỮ LIỆU CŨ ==========

        Map<Integer, String> oldStepImages = new HashMap<>();
        try {
            recipeStepRepository.findStepImagesByRecipeId(id).forEach(step -> {
                Integer num = (Integer) step.get("step_number");
                String url = (String) step.get("image_url");
                if (num != null && url != null) oldStepImages.put(num, url);
            });
        } catch (Exception e) {
            log.warn("Không lấy được ảnh steps cũ: {}", e.getMessage());
        }

        Map<UUID, Map<String, String>> oldIngredientMap = new HashMap<>();
        try {
            recipeIngredientRepository.findIngredientDetailsByRecipeId(id).forEach(ing -> {
                UUID ingredientId = (UUID) ing.get("ingredient_id");
                if (ingredientId != null) {
                    Map<String, String> details = new HashMap<>();
                    details.put("quantity", (String) ing.get("quantity"));
                    details.put("unit", (String) ing.get("unit"));
                    details.put("notes", (String) ing.get("notes"));
                    oldIngredientMap.put(ingredientId, details);
                }
            });
        } catch (Exception e) {
            log.warn("Không lấy được ingredient details cũ: {}", e.getMessage());
        }

        List<UUID> oldTagIds = new ArrayList<>();
        try {
            recipeTagRepository.findTagIdsByRecipeId(id).forEach(tag -> {
                UUID tagId = (UUID) tag.get("tag_id");
                if (tagId != null) oldTagIds.add(tagId);
            });
        } catch (Exception e) {
            log.warn("Không lấy được tags cũ: {}", e.getMessage());
        }

        List<UUID> oldCategoryIds = new ArrayList<>();
        try {
            recipeCategoryRepository.findCategoryIdsByRecipeId(id).forEach(cat -> {
                UUID categoryId = (UUID) cat.get("category_id");
                if (categoryId != null) oldCategoryIds.add(categoryId);
            });
        } catch (Exception e) {
            log.warn("Không lấy được categories cũ: {}", e.getMessage());
        }

        // ========== ẢNH ĐẠI DIỆN ==========

        if (image != null && !image.isEmpty()) {
            if (recipe.getFeaturedImage() != null) {
                fileStorageService.deleteFile(recipe.getFeaturedImage());
            }
            String newImageUrl = fileStorageService.uploadFile(image);
            request.setFeaturedImage(newImageUrl);
            log.info("📸 Cập nhật ảnh đại diện mới: {}", newImageUrl);
        } else {
            request.setFeaturedImage(recipe.getFeaturedImage());
        }

        // ========== ẢNH BƯỚC NẤU ==========

        log.info("📷 Tổng số step images từ client: {}", stepImages != null ? stepImages.size() : 0);

        if (request.getSteps() != null) {
            Map<Integer, String> newStepImages = new HashMap<>();
            if (stepImages != null && !stepImages.isEmpty()) {
                newStepImages = mapStepImages(stepImages);
            }

            for (int i = 0; i < request.getSteps().size(); i++) {
                var step = request.getSteps().get(i);
                Integer stepNumber = step.getStepNumber() != null ? step.getStepNumber() : (i + 1);

                if (newStepImages.containsKey(stepNumber)) {
                    step.setImageUrl(newStepImages.get(stepNumber));
                    log.info("🆕 Step {} dùng ảnh mới", stepNumber);
                } else if (step.getImageUrl() == null && oldStepImages.containsKey(stepNumber)) {
                    step.setImageUrl(oldStepImages.get(stepNumber));
                    log.info("↩️ Step {} giữ ảnh cũ", stepNumber);
                }
            }
        }

        // ========== INGREDIENT DETAILS ==========

        if (request.getIngredientDetails() != null && !request.getIngredientDetails().isEmpty()) {
            for (var detail : request.getIngredientDetails()) {
                UUID ingredientId = detail.getIngredientId();

                if (detail.getQuantity() == null && oldIngredientMap.containsKey(ingredientId)) {
                    Map<String, String> oldDetails = oldIngredientMap.get(ingredientId);
                    String oldQuantity = oldDetails.get("quantity");
                    if (oldQuantity != null) {
                        try {
                            detail.setQuantity(Double.parseDouble(oldQuantity));
                        } catch (NumberFormatException e) {
                            log.warn("Không parse được quantity cũ: {}", oldQuantity);
                        }
                    }
                    detail.setUnit(oldDetails.get("unit"));
                    detail.setNotes(oldDetails.get("notes"));
                    log.info("Giữ lại ingredient {} quantity/unit cũ", ingredientId);
                }
            }
        } else {
            // Nếu không gửi gì -> giữ nguyên toàn bộ nguyên liệu cũ
            request.setIngredientDetails(oldIngredientMap.entrySet().stream().map(entry -> {
                UUID ingredientId = entry.getKey();
                Map<String, String> details = entry.getValue();
                var dto = new com.backend.cookshare.recipe_management.dto.request.IngredientDetailRequest();
                dto.setIngredientId(ingredientId);
                if (details.get("quantity") != null) {
                    try {
                        dto.setQuantity(Double.parseDouble(details.get("quantity")));
                    } catch (NumberFormatException ignored) {}
                }
                dto.setUnit(details.get("unit"));
                dto.setNotes(details.get("notes"));
                return dto;
            }).toList());
            log.info("↩️ Giữ nguyên toàn bộ nguyên liệu cũ ({} items)", request.getIngredientDetails().size());
        }

        // ========== TAGS & CATEGORIES ==========

        if (request.getTagIds() == null || request.getTagIds().isEmpty()) {
            request.setTagIds(oldTagIds);
            log.info("↩️ Giữ lại {} tags cũ", oldTagIds.size());
        }

        if (request.getCategoryIds() == null || request.getCategoryIds().isEmpty()) {
            request.setCategoryIds(oldCategoryIds);
            log.info("↩️ Giữ lại {} categories cũ", oldCategoryIds.size());
        }

        // ========== CẬP NHẬT THÔNG TIN RECIPE ==========

        RecipeStatus oldStatus = recipe.getStatus();

        recipeMapper.updateRecipeFromDto(request, recipe);
        recipe.setUpdatedAt(LocalDateTime.now());

        // Giữ lại status cũ nếu request không gửi
        if (request.getStatus() == null) {
            recipe.setStatus(oldStatus);
            log.info("Giữ lại status cũ: {}", oldStatus);
        }

        // Cập nhật slug nếu tiêu đề thay đổi
        if (request.getTitle() != null && !request.getTitle().equalsIgnoreCase(recipe.getTitle())) {
            recipe.setSlug(generateSlug(request.getTitle()));
        } else if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        Recipe updatedRecipe = recipeRepository.save(recipe);

        // ========== XÓA QUAN HỆ CŨ VÀ LƯU LẠI ==========

        log.info("🧹 Xóa và tái tạo lại các quan hệ recipe: {}", id);

        recipeStepRepository.deleteAllByRecipeId(id);
        recipeIngredientRepository.deleteAllByRecipeId(id);
        recipeTagRepository.deleteAllByRecipeId(id);
        recipeCategoryRepository.deleteAllByRecipeId(id);

        saveRecipeRelations(id, request);

        log.info("✅ Recipe {} cập nhật thành công", id);

        return loadRecipeResponse(updatedRecipe);
    }


    // ================= READ / DELETE =================

    @Override
    public RecipeResponse getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));
        return loadRecipeResponse(recipe);
    }

    @Override
    @Transactional
    public void deleteRecipe(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        if (recipe.getFeaturedImage() != null) {
            fileStorageService.deleteFile(recipe.getFeaturedImage());
        }

        // Xóa các liên kết phụ
        recipeStepRepository.deleteAllByRecipeId(id);
        recipeIngredientRepository.deleteAllByRecipeId(id);
        recipeTagRepository.deleteAllByRecipeId(id);
        recipeCategoryRepository.deleteAllByRecipeId(id);

        recipeRepository.deleteById(id);
    }

    @Override
    public Page<RecipeResponse> getAllRecipes(Pageable pageable) {
        return recipeRepository.findAll(pageable).map(recipe -> {
            RecipeResponse response = recipeMapper.toResponse(recipe);
            convertImageUrlsToFirebase(response);
            return response;
        });
    }

    @Override
    public List<RecipeResponse> getAllRecipesByUserId(UUID userId) {
        List<Recipe> recipes = recipeRepository.findByUserIdAndStatus(userId, RecipeStatus.APPROVED);
        if (recipes == null || recipes.isEmpty()) return Collections.emptyList();
        return recipes.stream()
                .map(recipe -> {
                    RecipeResponse response = recipeMapper.toResponse(recipe);
                    convertImageUrlsToFirebase(response);
                    return response;
                })
                .toList();
    }

    // ================= HELPERS =================

    private RecipeResponse loadRecipeResponse(Recipe recipe) {
        RecipeDetailsResult details = recipeLoaderHelper.loadRecipeDetailsForPublic(
                recipe.getRecipeId(), recipe.getUserId());

        RecipeResponse response = recipeMapper.toResponse(recipe);
        response.setSteps(details.steps);
        response.setIngredients(details.ingredients);
        response.setTags(details.tags);
        response.setCategories(details.categories);
        response.setFullName(details.fullName);

        // Convert tất cả URL ảnh sang Firebase URL đầy đủ
        convertImageUrlsToFirebase(response);

        return response;
    }

    private void convertImageUrlsToFirebase(RecipeResponse response) {
        // Convert featured image
        if (response.getFeaturedImage() != null) {
            response.setFeaturedImage(fileStorageService.convertPathToFirebaseUrl(response.getFeaturedImage()));
        }

        // Convert step images
        if (response.getSteps() != null) {
            response.getSteps().forEach(step -> {
                if (step.getImageUrl() != null) {
                    step.setImageUrl(fileStorageService.convertPathToFirebaseUrl(step.getImageUrl()));
                }
            });
        }
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private Map<Integer, String> mapStepImages(List<MultipartFile> stepImages) {
        Map<Integer, String> stepImageMap = new HashMap<>();
        Pattern pattern = Pattern.compile("step_(\\d+)\\.");

        for (MultipartFile stepImage : stepImages) {
            if (stepImage != null && !stepImage.isEmpty()) {
                String originalFilename = stepImage.getOriginalFilename();
                if (originalFilename != null) {
                    Matcher matcher = pattern.matcher(originalFilename);
                    if (matcher.find()) {
                        try {
                            int stepNumber = Integer.parseInt(matcher.group(1));
                            String uploadedUrl = fileStorageService.uploadFile(stepImage);
                            stepImageMap.put(stepNumber, uploadedUrl);
                            log.info("Uploaded step {} image: {}", stepNumber, uploadedUrl);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid step number in filename: {}", originalFilename);
                        }
                    } else {
                        log.warn("Filename doesn't match step pattern: {}", originalFilename);
                    }
                }
            }
        }

        return stepImageMap;
    }

    private void saveRecipeRelations(UUID recipeId, RecipeRequest request) {
        // Lưu bước nấu
        if (request.getSteps() != null) {
            request.getSteps().forEach(step ->
                    recipeStepRepository.insertRecipeStep(
                            recipeId,
                            step.getStepNumber(),
                            step.getInstruction(),
                            step.getImageUrl(),
                            step.getVideoUrl(),
                            step.getEstimatedTime(),
                            step.getTips()
                    )
            );
        }

        // Lưu nguyên liệu
        // Ưu tiên lưu từ ingredientDetails (có đầy đủ quantity + unit)
        if (request.getIngredientDetails() != null && !request.getIngredientDetails().isEmpty()) {
            request.getIngredientDetails().forEach(detail ->
                    recipeIngredientRepository.insertRecipeIngredient(
                            recipeId,
                            detail.getIngredientId(),
                            detail.getQuantity().toString(),  // ✅ Lưu quantity
                            detail.getUnit(),                 // ✅ Lưu unit
                            detail.getNotes(),
                            detail.getOrderIndex()
                    )
            );
        } else if (request.getIngredients() != null) {
            // Fallback cho API cũ
            request.getIngredients().forEach(ingredientId ->
                    recipeIngredientRepository.insertRecipeIngredient(
                            recipeId, ingredientId, null, null, null, null
                    )
            );
        }

        // Lưu tag
        if (request.getTagIds() != null) {
            request.getTagIds().forEach(tagId ->
                    recipeTagRepository.insertRecipeTag(recipeId, tagId)
            );
        }

        // Lưu danh mục
        if (request.getCategoryIds() != null) {
            request.getCategoryIds().forEach(categoryId ->
                    recipeCategoryRepository.insertRecipeCategory(recipeId, categoryId)
            );
        }
    }
}