package com.backend.cookshare.recipe_management.service.impl;

import com.backend.cookshare.common.exception.CustomException;
import com.backend.cookshare.common.exception.ErrorCode;
import com.backend.cookshare.recipe_management.dto.request.RecipeRequest;
import com.backend.cookshare.recipe_management.dto.request.CategoryRequest;
import com.backend.cookshare.recipe_management.dto.request.TagRequest;
import com.backend.cookshare.recipe_management.dto.request.IngredientRequest;
import com.backend.cookshare.recipe_management.dto.response.RecipeDetailsResult;
import com.backend.cookshare.recipe_management.dto.response.RecipeResponse;
import com.backend.cookshare.recipe_management.entity.Recipe;
import com.backend.cookshare.recipe_management.entity.Category;
import com.backend.cookshare.recipe_management.entity.Tag;
import com.backend.cookshare.recipe_management.entity.Ingredient;
import com.backend.cookshare.recipe_management.enums.RecipeStatus;
import com.backend.cookshare.recipe_management.mapper.RecipeMapper;
import com.backend.cookshare.recipe_management.mapper.CategoryMapper;
import com.backend.cookshare.recipe_management.mapper.TagMapper;
import com.backend.cookshare.recipe_management.mapper.IngredientMapper;
import com.backend.cookshare.recipe_management.repository.*;
import com.backend.cookshare.authentication.service.FirebaseStorageService;
import com.backend.cookshare.recipe_management.service.RecipeService;
import com.backend.cookshare.user.service.ActivityLogService;
import com.backend.cookshare.user.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeTagRepository recipeTagRepository;
    private final RecipeCategoryRepository recipeCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeMapper recipeMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final IngredientMapper ingredientMapper;
    private final RecipeLoaderHelper recipeLoaderHelper;
    private final FirebaseStorageService fileStorageService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final com.backend.cookshare.authentication.util.SecurityUtil securityUtil;
    private final com.backend.cookshare.system.repository.ReportQueryRepository reportQueryRepository;
    private final com.backend.cookshare.authentication.repository.UserRepository userRepository;

    // ================= CREATE WITH BATCH SUPPORT =================

    @Override
    @Transactional
    public RecipeResponse createRecipeWithFiles(RecipeRequest request, MultipartFile image,
            List<MultipartFile> stepImages) {
        // Upload ảnh đại diện nếu có
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.uploadFile(image);
            request.setFeaturedImage(imageUrl);
        }

        // Upload và map ảnh cho từng bước theo stepNumber
        if (request.getSteps() != null && stepImages != null && !stepImages.isEmpty()) {
            Map<Integer, String> stepImageMap = mapStepImages(stepImages);
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
        log.info("🚀 Bắt đầu tạo recipe với batch data");

        // 1️⃣ TẠO CÁC CATEGORIES MỚI (nếu có)
        List<UUID> finalCategoryIds = new ArrayList<>();
        if (request.getNewCategories() != null && !request.getNewCategories().isEmpty()) {
            log.info("📁 Tạo {} categories mới", request.getNewCategories().size());
            for (CategoryRequest catReq : request.getNewCategories()) {
                Category category = createCategoryIfNotExists(catReq);
                finalCategoryIds.add(category.getCategoryId());
            }
        }
        // Thêm các category đã có sẵn
        if (request.getCategoryIds() != null) {
            finalCategoryIds.addAll(request.getCategoryIds());
        }
        request.setCategoryIds(finalCategoryIds);

        // 2️⃣ TẠO CÁC TAGS MỚI (nếu có)
        List<UUID> finalTagIds = new ArrayList<>();
        if (request.getNewTags() != null && !request.getNewTags().isEmpty()) {
            log.info("🏷️ Tạo {} tags mới", request.getNewTags().size());
            for (TagRequest tagReq : request.getNewTags()) {
                Tag tag = createTagIfNotExists(tagReq);
                finalTagIds.add(tag.getTagId());
            }
        }
        // Thêm các tag đã có sẵn
        if (request.getTagIds() != null) {
            finalTagIds.addAll(request.getTagIds());
        }
        request.setTagIds(finalTagIds);

        // 3️⃣ TẠO CÁC INGREDIENTS MỚI (nếu có)
        if (request.getNewIngredients() != null && !request.getNewIngredients().isEmpty()) {
            log.info("🥕 Tạo {} ingredients mới", request.getNewIngredients().size());

            // Collect created ingredient UUIDs and append to request.ingredients
            List<UUID> createdIngredientIds = new ArrayList<>();
            for (IngredientRequest ingReq : request.getNewIngredients()) {
                Ingredient ingredient = createIngredientIfNotExists(ingReq);
                createdIngredientIds.add(ingredient.getIngredientId());
                log.info("Created ingredient {} -> {}", ingredient.getName(), ingredient.getIngredientId());
            }

            // Merge created ingredient IDs into request.ingredients so saveRecipeRelations
            // can persist them
            List<UUID> mergedIngredients = new ArrayList<>();
            if (request.getIngredients() != null) {
                mergedIngredients.addAll(request.getIngredients());
            }
            mergedIngredients.addAll(createdIngredientIds);
            request.setIngredients(mergedIngredients);

        }

        // 5️⃣ TẠO RECIPE VỚI DỮ LIỆU ĐÃ HOÀN CHỈNH
        Recipe recipe = recipeMapper.toEntity(request);

        if (recipe.getSlug() == null || recipe.getSlug().isEmpty()) {
            recipe.setSlug(generateSlug(recipe.getTitle()));
        }

        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());

        Recipe savedRecipe = recipeRepository.save(recipe);
        UUID recipeId = savedRecipe.getRecipeId();

        saveRecipeRelations(recipeId, request);
        
        // Log activity và update stats bất đồng bộ (không block response)
        postRecipeCreationAsync(savedRecipe.getUserId(), recipeId);

        log.info("✅ Recipe {} tạo thành công với tất cả dữ liệu mới", recipeId);

        return loadRecipeResponse(savedRecipe);
    }

    @Async
    public void postRecipeCreationAsync(UUID userId, UUID recipeId) {
        try {
            // Log activity
            activityLogService.logRecipeActivityAsync(userId, recipeId, "CREATE");

            // Tăng recipe_count
            userRepository.incrementRecipeCount(userId);
            
            log.debug("Post-creation tasks completed for recipe {}", recipeId);
        } catch (Exception e) {
            log.warn("Error in post-creation tasks for recipe {}: {}", recipeId, e.getMessage());
        }
    }


    private Category createCategoryIfNotExists(CategoryRequest request) {
        // Kiểm tra đã tồn tại chưa (theo tên)
        Optional<Category> existing = categoryRepository.findByName(request.getName());
        if (existing.isPresent()) {
            log.info("Category '{}' đã tồn tại, sử dụng lại", request.getName());
            return existing.get();
        }

        // Tạo mới
        Category category = categoryMapper.toEntity(request);
        category.setSlug(generateSlugVietnamese(request.getName()));
        category.setCreatedAt(LocalDateTime.now());

        Category saved = categoryRepository.save(category);
        log.info("✅ Đã tạo category mới: {} ({})", saved.getName(), saved.getCategoryId());
        return saved;
    }

    // ================= HELPER: TẠO TAG NẾU CHƯA TỒN TẠI =================

    private Tag createTagIfNotExists(TagRequest request) {
        // Kiểm tra đã tồn tại chưa
        if (tagRepository.existsByNameIgnoreCase(request.getName())) {
            Optional<Tag> existing = tagRepository.findByNameIgnoreCase(request.getName());
            if (existing.isPresent()) {
                log.info("Tag '{}' đã tồn tại, sử dụng lại", request.getName());
                return existing.get();
            }
        }

        // Tạo mới
        Tag tag = tagMapper.toEntity(request);
        tag.setSlug(generateSlug(request.getName()));
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUsageCount(0);

        Tag saved = tagRepository.save(tag);
        log.info("✅ Đã tạo tag mới: {} ({})", saved.getName(), saved.getTagId());
        return saved;
    }

    // ================= HELPER: TẠO INGREDIENT NẾU CHƯA TỒN TẠI =================

    private Ingredient createIngredientIfNotExists(IngredientRequest request) {
        // Kiểm tra đã tồn tại chưa
        Optional<Ingredient> existing = ingredientRepository.findByNameIgnoreCase(request.getName());
        if (existing.isPresent()) {
            log.info("Ingredient '{}' đã tồn tại, sử dụng lại", request.getName());
            return existing.get();
        }

        // Tạo mới
        Ingredient ingredient = ingredientMapper.toEntity(request);
        ingredient.setSlug(generateSlug(request.getName()));
        ingredient.setCreatedAt(LocalDateTime.now());
        ingredient.setUsageCount(0);

        Ingredient saved = ingredientRepository.save(ingredient);
        log.info("✅ Đã tạo ingredient mới: {} ({})", saved.getName(), saved.getIngredientId());
        return saved;
    }

    @Override
    @Transactional
    public RecipeResponse updateRecipe(UUID id, RecipeRequest request,
            MultipartFile image, List<MultipartFile> stepImages) {
        Recipe recipe = recipeRepository.findRecipeEdit(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND, "Không tìm thấy recipe id: " + id));

        log.info("Bắt đầu cập nhật recipe: {}", id);

        // ========== LẤY DỮ LIỆU CŨ ==========

        Map<Integer, String> oldStepImages = new HashMap<>();
        try {
            recipeStepRepository.findStepImagesByRecipeId(id).forEach(step -> {
                Integer num = (Integer) step.get("step_number");
                String url = (String) step.get("image_url");
                if (num != null && url != null)
                    oldStepImages.put(num, url);
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
                if (tagId != null)
                    oldTagIds.add(tagId);
            });
        } catch (Exception e) {
            log.warn("Không lấy được tags cũ: {}", e.getMessage());
        }

        List<UUID> oldCategoryIds = new ArrayList<>();
        try {
            recipeCategoryRepository.findCategoryIdsByRecipeId(id).forEach(cat -> {
                UUID categoryId = (UUID) cat.get("category_id");
                if (categoryId != null)
                    oldCategoryIds.add(categoryId);
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
                    log.info("Step {} dùng ảnh mới", stepNumber);
                } else if (step.getImageUrl() == null && oldStepImages.containsKey(stepNumber)) {
                    step.setImageUrl(oldStepImages.get(stepNumber));
                    log.info("Step {} giữ ảnh cũ", stepNumber);
                }
            }
        }

        // ========== TẠO CATEGORIES MỚI (nếu có) ==========

        List<UUID> finalCategoryIds = new ArrayList<>();
        if (request.getNewCategories() != null && !request.getNewCategories().isEmpty()) {
            log.info(" Tạo {} categories mới", request.getNewCategories().size());
            for (CategoryRequest catReq : request.getNewCategories()) {
                Category category = createCategoryIfNotExists(catReq);
                finalCategoryIds.add(category.getCategoryId());
            }
        }
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            finalCategoryIds.addAll(request.getCategoryIds());
        } else if (finalCategoryIds.isEmpty()) {
            finalCategoryIds.addAll(oldCategoryIds);
            log.info(" Giữ lại {} categories cũ", oldCategoryIds.size());
        }
        request.setCategoryIds(finalCategoryIds);

        // ========== TẠO TAGS MỚI (nếu có) ==========

        List<UUID> finalTagIds = new ArrayList<>();
        if (request.getNewTags() != null && !request.getNewTags().isEmpty()) {
            log.info("Tạo {} tags mới", request.getNewTags().size());
            for (TagRequest tagReq : request.getNewTags()) {
                Tag tag = createTagIfNotExists(tagReq);
                finalTagIds.add(tag.getTagId());
            }
        }
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            finalTagIds.addAll(request.getTagIds());
        } else if (finalTagIds.isEmpty()) {
            finalTagIds.addAll(oldTagIds);
            log.info("Giữ lại {} tags cũ", oldTagIds.size());
        }
        request.setTagIds(finalTagIds);

        // ========== TẠO INGREDIENTS MỚI (nếu có) ==========

        if (request.getNewIngredients() != null && !request.getNewIngredients().isEmpty()) {
            log.info("Tạo {} ingredients mới", request.getNewIngredients().size());

            List<UUID> createdIngredientIds = new ArrayList<>();
            for (IngredientRequest ingReq : request.getNewIngredients()) {
                Ingredient ingredient = createIngredientIfNotExists(ingReq);
                createdIngredientIds.add(ingredient.getIngredientId());
                log.info("Created ingredient {} -> {}", ingredient.getName(), ingredient.getIngredientId());
            }

            List<UUID> mergedIngredients = new ArrayList<>();
            if (request.getIngredients() != null) {
                mergedIngredients.addAll(request.getIngredients());
            }
            mergedIngredients.addAll(createdIngredientIds);
            request.setIngredients(mergedIngredients);
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
            request.setIngredientDetails(oldIngredientMap.entrySet().stream().map(entry -> {
                UUID ingredientId = entry.getKey();
                Map<String, String> details = entry.getValue();
                var dto = new com.backend.cookshare.recipe_management.dto.request.IngredientDetailRequest();
                dto.setIngredientId(ingredientId);
                if (details.get("quantity") != null) {
                    try {
                        dto.setQuantity(Double.parseDouble(details.get("quantity")));
                    } catch (NumberFormatException ignored) {
                    }
                }
                dto.setUnit(details.get("unit"));
                dto.setNotes(details.get("notes"));
                return dto;
            }).toList());
            log.info("Giữ nguyên toàn bộ nguyên liệu cũ ({} items)", request.getIngredientDetails().size());
        }

        // ========== CẬP NHẬT THÔNG TIN RECIPE ==========

        RecipeStatus oldStatus = recipe.getStatus();

        recipeMapper.updateRecipeFromDto(request, recipe);
        recipe.setUpdatedAt(LocalDateTime.now());

        if (request.getStatus() == null) {
            recipe.setStatus(oldStatus);
            log.info("Giữ lại status cũ: {}", oldStatus);
        }

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

        // Log activity bất đồng bộ
        activityLogService.logRecipeActivityAsync(updatedRecipe.getUserId(), id, "UPDATE");

        log.info("Recipe {} cập nhật thành công", id);

        return loadRecipeResponse(updatedRecipe);
    }

    // ================= READ / DELETE (GIỮ NGUYÊN) =================

    @Override
    public RecipeResponse getRecipeById(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        incrementViewCountAsync(id);

        try {
            UUID userId = getCurrentUserIdOrNull();
            if (userId != null) {
                activityLogService.logViewActivityAsync(userId, id);
            }
        } catch (Exception e) {
            log.debug("Không thể log view activity: {}", e.getMessage());
        }

        // Load response và tăng viewCount trong response để frontend hiển thị ngay
        RecipeResponse response = loadRecipeResponse(recipe);
        response.setViewCount(recipe.getViewCount() + 1);

        return response;
    }

    @Async
    public void incrementViewCountAsync(UUID recipeId) {
        try {
            recipeRepository.incrementViewCount(recipeId);
            log.debug("Incremented view count for recipe {}", recipeId);
        } catch (Exception e) {
            log.warn("Không thể tăng view count cho recipe {}: {}", recipeId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteRecipe(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));
        notificationService.deleteRecipeNotifications(id);

        if (recipe.getFeaturedImage() != null) {
            fileStorageService.deleteFile(recipe.getFeaturedImage());
        }

        recipeStepRepository.deleteAllByRecipeId(id);
        recipeIngredientRepository.deleteAllByRecipeId(id);
        recipeTagRepository.deleteAllByRecipeId(id);
        recipeCategoryRepository.deleteAllByRecipeId(id);

        recipeRepository.deleteById(id);
        
        postRecipeDeletionAsync(recipe.getUserId(), id);
    }

    @Async
    public void postRecipeDeletionAsync(UUID userId, UUID recipeId) {
        try {
            // Log activity
            activityLogService.logRecipeActivityAsync(userId, recipeId, "DELETE");
            
            // Giảm recipe_count
            userRepository.decrementRecipeCount(userId);
            
            log.debug("Post-deletion tasks completed for recipe {}", recipeId);
        } catch (Exception e) {
            log.warn("Error in post-deletion tasks for recipe {}: {}", recipeId, e.getMessage());
        }
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
    public List<RecipeResponse> getAllRecipesByUserId(UUID userId, UUID currentUserId, boolean includeAll) {
        List<Recipe> recipes;

        // Nếu là chủ sở hữu: hiển tất cả (cả công khai và riêng tư)
        if (currentUserId != null && currentUserId.equals(userId)) {
            if (includeAll) {
                // Lấy tất cả recipes (bao gồm cả PENDING và APPROVED)
                recipes = recipeRepository.findByUserId(userId);
            } else {
                // Chỉ lấy recipes đã được APPROVED
                recipes = recipeRepository.findByUserIdAndStatus(userId, RecipeStatus.APPROVED);
            }
        } else {
            // Nếu là người khác: chỉ hiển công khai và đã APPROVED
            recipes = recipeRepository.findByUserIdAndStatusAndIsPublished(userId, RecipeStatus.APPROVED, true);
        }

        if (recipes == null || recipes.isEmpty())
            return Collections.emptyList();
        return recipes.stream()
                .map(recipe -> {
                    RecipeResponse response = recipeMapper.toResponse(recipe);
                    convertImageUrlsToFirebase(response);
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional
    public RecipeResponse togglePrivacy(UUID id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.RECIPE_NOT_FOUND));

        // Chỉ cho phép toggle privacy nếu recipe đã được APPROVED
        if (recipe.getStatus() != RecipeStatus.APPROVED) {
            throw new CustomException(ErrorCode.RECIPE_NOT_APPROVED);
        }

        // Toggle trạng thái is_published (công khai/riêng tư)
        // Xử lý null-safe: nếu null thì coi như true (mặc định công khai)
        Boolean currentPublished = recipe.getIsPublished();
        if (currentPublished == null) {
            currentPublished = true;
        }
        recipe.setIsPublished(!currentPublished);
        recipeRepository.save(recipe);

        RecipeResponse response = recipeMapper.toResponse(recipe);
        convertImageUrlsToFirebase(response);
        return response;
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

        convertImageUrlsToFirebase(response);
        return response;
    }

    private void convertImageUrlsToFirebase(RecipeResponse response) {
        if (response.getFeaturedImage() != null) {
            response.setFeaturedImage(fileStorageService.convertPathToFirebaseUrl(response.getFeaturedImage()));
        }

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

    private String generateSlugVietnamese(String input) {
        if (input == null)
            return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
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
                    }
                }
            }
        }

        return stepImageMap;
    }

    private void saveRecipeRelations(UUID recipeId, RecipeRequest request) {
        // Lưu bước nấu
        if (request.getSteps() != null) {
            request.getSteps().forEach(step -> recipeStepRepository.insertRecipeStep(
                    recipeId,
                    step.getStepNumber(),
                    step.getInstruction(),
                    step.getImageUrl(),
                    step.getVideoUrl(),
                    step.getEstimatedTime(),
                    step.getTips()));
        }

        // Lưu nguyên liệu
        if (request.getIngredientDetails() != null && !request.getIngredientDetails().isEmpty()) {
            request.getIngredientDetails().forEach(detail -> recipeIngredientRepository.insertRecipeIngredient(
                    recipeId,
                    detail.getIngredientId(),
                    detail.getQuantity().toString(),
                    detail.getUnit(),
                    detail.getNotes(),
                    detail.getOrderIndex()));
        } else if (request.getIngredients() != null) {
            request.getIngredients().forEach(ingredientId -> recipeIngredientRepository.insertRecipeIngredient(
                    recipeId, ingredientId, null, null, null, null));
        }

        // Lưu tag
        if (request.getTagIds() != null) {
            request.getTagIds().forEach(tagId -> recipeTagRepository.insertRecipeTag(recipeId, tagId));
        }

        // Lưu danh mục
        if (request.getCategoryIds() != null) {
            request.getCategoryIds()
                    .forEach(categoryId -> recipeCategoryRepository.insertRecipeCategory(recipeId, categoryId));
        }
    }

    private UUID getCurrentUserIdOrNull() {
        try {
            String username = securityUtil.getCurrentUserLogin().orElse(null);
            if (username == null) {
                return null;
            }
            return reportQueryRepository.findUserIdByUsername(username).orElse(null);
        } catch (Exception e) {
            log.debug("Không thể lấy userId: {}", e.getMessage());
            return null;
        }
    }
}