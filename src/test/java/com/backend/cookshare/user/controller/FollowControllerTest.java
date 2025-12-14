package com.backend.cookshare.user.controller;

import com.backend.cookshare.common.dto.PageResponse;
import com.backend.cookshare.interaction.dto.response.RecipeSummaryResponse;
import com.backend.cookshare.user.dto.*;
import com.backend.cookshare.user.service.FollowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FollowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FollowService followService;

    @InjectMocks
    private FollowController followController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID userId = UUID.randomUUID();
    private final UUID followingId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();

    private FollowRequest createFollowRequest() {
        FollowRequest request = new FollowRequest();
        request.setFollowingId(followingId);
        return request;
    }

    private FollowResponse createFollowResponse() {
        FollowResponse response = new FollowResponse();
        response.setFollowerId(userId);
        response.setFollowingId(followingId);
        response.setMessage("Bạn đã follow thành công");
        return response;
    }

    private UserFollowDto createUserFollowDto() {
        UserFollowDto dto = new UserFollowDto();
        dto.setUserId(followingId);
        dto.setUsername("Nguyen Duy");
        return dto;
    }

    private PageResponse<UserFollowDto> createUserFollowPage() {
        PageResponse<UserFollowDto> page = new PageResponse<>();
        page.setContent(Collections.singletonList(createUserFollowDto()));
        page.setPage(0);
        page.setSize(10);
        page.setTotalPages(1);
        return page;
    }

    private RecipeByFollowingResponse createRecipeByFollowingResponse() {
        RecipeSummaryResponse recipeSummary = RecipeSummaryResponse.builder()
                .recipeId(UUID.randomUUID())
                .title("Công thức món ăn")
                .build();

        RecipeByFollowingResponse response = RecipeByFollowingResponse.builder()
                .followerId(UUID.randomUUID())
                .followingId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .recipe(recipeSummary)
                .build();

        return response;
    }

    private PageResponse<RecipeByFollowingResponse> createRecipeByFollowingPage() {
        PageResponse<RecipeByFollowingResponse> page = new PageResponse<>();
        page.setContent(Collections.singletonList(createRecipeByFollowingResponse()));
        page.setPage(0);
        page.setSize(10);
        page.setTotalPages(1);
        return page;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(followController).build();
    }

    @Test
    void followUser_Success() throws Exception {
        FollowRequest request = createFollowRequest();
        FollowResponse response = createFollowResponse();
        when(followService.followUser(eq(userId), eq(followingId))).thenReturn(response);

        mockMvc.perform(post("/users/{userId}/follow", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.followerId").value(userId.toString()))
                .andExpect(jsonPath("$.data.followingId").value(followingId.toString()));

        verify(followService).followUser(eq(userId), eq(followingId));
    }

    @Test
    void unfollowUser_Success() throws Exception {
        doNothing().when(followService).unfollowUser(userId, followingId);

        mockMvc.perform(delete("/users/{userId}/follow/{followingId}", userId, followingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Unfollow thành công"));

        verify(followService).unfollowUser(userId, followingId);
    }

    @Test
    void getFollowers_Success() throws Exception {
        PageResponse<UserFollowDto> page = createUserFollowPage();
        when(followService.getFollowers(userId, 0, 10)).thenReturn(page);

        mockMvc.perform(get("/users/{userId}/followers", userId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(followingId.toString()));

        verify(followService).getFollowers(userId, 0, 10);
    }

    @Test
    void getFollowing_Success() throws Exception {
        PageResponse<UserFollowDto> page = createUserFollowPage();
        when(followService.getFollowing(userId, 0, 10)).thenReturn(page);

        mockMvc.perform(get("/users/{userId}/following", userId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(followingId.toString()));

        verify(followService).getFollowing(userId, 0, 10);
    }

    @Test
    void checkFollowStatus_Success() throws Exception {
        when(followService.isFollowing(userId, targetUserId)).thenReturn(true);

        mockMvc.perform(get("/users/{userId}/follow/check/{targetUserId}", userId, targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(followService).isFollowing(userId, targetUserId);
    }

    @Test
    void getRecipesByFollowing_Success() throws Exception {
        PageResponse<RecipeByFollowingResponse> page = createRecipeByFollowingPage();
        when(followService.getRecipesByFollowing(0, 10)).thenReturn(page);

        mockMvc.perform(get("/users/following/recipes")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].recipe.title").value("Công thức món ăn"));

        verify(followService).getRecipesByFollowing(0, 10);
    }
}
