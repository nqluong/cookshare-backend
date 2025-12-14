package com.backend.cookshare.user.controller;

import com.backend.cookshare.common.dto.ApiResponse;
import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.user.dto.*;
import com.backend.cookshare.user.service.CollectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CollectionControllerTest {

    private MockMvc mockMvc;

    private final CollectionService collectionService = mock(CollectionService.class);

    @InjectMocks
    private CollectionController collectionController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID userId = UUID.randomUUID();
    private final UUID collectionId = UUID.randomUUID();
    private final UUID recipeId = UUID.randomUUID();

    private CreateCollectionRequest createCollectionRequest() {
        CreateCollectionRequest req = new CreateCollectionRequest();
        req.setName("Bộ sưu tập món ngọt");
        req.setDescription("Các món ngọt hấp dẫn");
        req.setIsPublic(true);
        return req;
    }

    private UpdateCollectionRequest updateCollectionRequest() {
        UpdateCollectionRequest req = new UpdateCollectionRequest();
        req.setName("Bộ sưu tập món mặn");
        req.setDescription("Các món mặn hấp dẫn");
        req.setIsPublic(false);
        return req;
    }

    private CollectionResponse createCollectionResponse() {
        CollectionResponse resp = new CollectionResponse();
        resp.setCollectionId(collectionId);
        resp.setName("Bộ sưu tập món ngọt");
        resp.setDescription("Các món ngọt hấp dẫn");
        resp.setIsPublic(true);
        return resp;
    }

    private CollectionUserDto createCollectionUserDto() {
        CollectionUserDto dto = new CollectionUserDto();
        dto.setCollectionId(collectionId);
        dto.setName("Bộ sưu tập món ngọt");
        dto.setDescription("Các món ngọt hấp dẫn");
        dto.setIsPublic(true);
        return dto;
    }

    private PageResponse<CollectionUserDto> createPageResponse() {
        PageResponse<CollectionUserDto> page = new PageResponse<>();
        page.setContent(Collections.singletonList(createCollectionUserDto()));
        page.setTotalPages(1);
        page.setPage(0);
        page.setTotalElements(1);
        return page;
    }

    @BeforeEach
    void setUp() {
        collectionController = new CollectionController(collectionService, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(collectionController).build();
    }

    @Test
    void createCollectionJson_Success() throws Exception {
        CreateCollectionRequest request = createCollectionRequest();
        CollectionResponse response = createCollectionResponse();

        when(collectionService.createCollection(userId, request)).thenReturn(response);

        mockMvc.perform(post("/users/{userId}/collections", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tạo bộ sưu tập thành công"))
                .andExpect(jsonPath("$.data.collectionId").value(collectionId.toString()));

        verify(collectionService).createCollection(userId, request);
    }

    @Test
    void createCollectionWithImage_Success() throws Exception {
        CreateCollectionRequest request = createCollectionRequest();
        CollectionResponse response = createCollectionResponse();
        MockMultipartFile data = new MockMultipartFile("data", "",
                "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile image = new MockMultipartFile("coverImage", "cover.png",
                "image/png", "fake-image".getBytes());

        when(collectionService.createCollectionWithImage(eq(userId), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/users/{userId}/collections", userId)
                        .file(data)
                        .file(image)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.collectionId").value(collectionId.toString()));

        verify(collectionService).createCollectionWithImage(eq(userId), any(), any());
    }

    @Test
    void updateCollectionJson_Success() throws Exception {
        UpdateCollectionRequest request = updateCollectionRequest();
        CollectionResponse response = createCollectionResponse();

        when(collectionService.updateCollection(collectionId, userId, request)).thenReturn(response);

        mockMvc.perform(put("/users/{userId}/collections/{collectionId}", userId, collectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật bộ sưu tập thành công"))
                .andExpect(jsonPath("$.data.collectionId").value(collectionId.toString()));

        verify(collectionService).updateCollection(collectionId, userId, request);
    }

    @Test
    void updateCollectionWithImage_Success() throws Exception {
        UpdateCollectionRequest request = updateCollectionRequest();
        CollectionResponse response = createCollectionResponse();

        MockMultipartFile data = new MockMultipartFile("data", "",
                "application/json", objectMapper.writeValueAsBytes(request));
        MockMultipartFile image = new MockMultipartFile("coverImage", "cover.png",
                "image/png", "fake-image".getBytes());

        when(collectionService.updateCollectionWithImage(eq(collectionId), eq(userId), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/users/{userId}/collections/{collectionId}", userId, collectionId)
                        .file(data)
                        .file(image)
                        .with(requestPostProcessor -> { requestPostProcessor.setMethod("PUT"); return requestPostProcessor; })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.collectionId").value(collectionId.toString()));

        verify(collectionService).updateCollectionWithImage(eq(collectionId), eq(userId), any(), any());
    }

    @Test
    void deleteCollection_Success() throws Exception {
        doNothing().when(collectionService).deleteCollection(collectionId, userId);

        mockMvc.perform(delete("/users/{userId}/collections/{collectionId}", userId, collectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa bộ sưu tập thành công"));

        verify(collectionService).deleteCollection(collectionId, userId);
    }

    @Test
    void getUserCollections_Success() throws Exception {
        PageResponse<CollectionUserDto> pageResponse = createPageResponse();
        when(collectionService.getUserCollections(userId, 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/users/{userId}/collections", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lấy danh sách bộ sưu tập thành công"))
                .andExpect(jsonPath("$.data.content[0].collectionId").value(collectionId.toString()));

        verify(collectionService).getUserCollections(userId, 0, 10);
    }

    @Test
    void getPublicCollections_Success() throws Exception {
        PageResponse<CollectionUserDto> pageResponse = createPageResponse();
        when(collectionService.getPublicUserCollections(userId, 0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/users/{userId}/collections/public", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lấy danh sách bộ sưu tập công khai thành công"))
                .andExpect(jsonPath("$.data.content[0].collectionId").value(collectionId.toString()));

        verify(collectionService).getPublicUserCollections(userId, 0, 10);
    }

    @Test
    void getCollectionDetail_Success() throws Exception {
        CollectionUserDto dto = createCollectionUserDto();
        when(collectionService.getCollectionDetail(collectionId, userId)).thenReturn(dto);

        mockMvc.perform(get("/users/{userId}/collections/{collectionId}", userId, collectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lấy chi tiết bộ sưu tập thành công"))
                .andExpect(jsonPath("$.data.collectionId").value(collectionId.toString()));

        verify(collectionService).getCollectionDetail(collectionId, userId);
    }

    @Test
    void addRecipeToCollection_Success() throws Exception {
        AddRecipeToCollectionRequest request = new AddRecipeToCollectionRequest();
        request.setRecipeId(recipeId);

        doNothing().when(collectionService).addRecipeToCollection(collectionId, userId, recipeId);

        mockMvc.perform(post("/users/{userId}/collections/{collectionId}/recipes", userId, collectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Thêm công thức vào bộ sưu tập thành công"));

        verify(collectionService).addRecipeToCollection(collectionId, userId, recipeId);
    }

    @Test
    void removeRecipeFromCollection_Success() throws Exception {
        doNothing().when(collectionService).removeRecipeFromCollection(collectionId, userId, recipeId);

        mockMvc.perform(delete("/users/{userId}/collections/{collectionId}/recipes/{recipeId}", userId, collectionId, recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa công thức khỏi bộ sưu tập thành công"));

        verify(collectionService).removeRecipeFromCollection(collectionId, userId, recipeId);
    }

    @Test
    void getCollectionRecipes_Success() throws Exception {
        PageResponse<CollectionRecipeDto> pageResponse = PageResponse.<CollectionRecipeDto>builder()
                .content(Collections.emptyList())
                .page(0)
                .totalPages(0)
                .totalElements(0L)
                .build();
        when(collectionService.getCollectionRecipes(collectionId, userId, 0, 20)).thenReturn(pageResponse);

        mockMvc.perform(get("/users/{userId}/collections/{collectionId}/recipes", userId, collectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Lấy danh sách công thức trong bộ sưu tập thành công"));

        verify(collectionService).getCollectionRecipes(collectionId, userId, 0, 20);
    }
}
