package com.devoops.user.controller;

import com.devoops.user.config.RoleAuthorizationInterceptor;
import com.devoops.user.config.UserContextResolver;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.entity.Role;
import com.devoops.user.exception.GlobalExceptionHandler;
import com.devoops.user.exception.UserNotFoundException;
import com.devoops.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserPublicControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserPublicController userPublicController;

    private MockMvc mockMvc;

    private UUID userId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userPublicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new UserContextResolver())
                .addInterceptors(new RoleAuthorizationInterceptor())
                .build();

        userId = UUID.randomUUID();

        userResponse = new UserResponse(
                userId,
                "testuser",
                "test@example.com",
                "Test",
                "User",
                "Test City",
                Role.GUEST
        );
    }

    @Nested
    @DisplayName("GET /api/user/{id} — getById")
    class GetByIdTests {

        @Test
        @DisplayName("Should return 200 OK with UserResponse when caller is a HOST")
        void getById_WithHostRole_ReturnsUserResponse() throws Exception {
            // Given
            when(userService.getProfile(eq(userId))).thenReturn(userResponse);

            // When/Then
            mockMvc.perform(get("/api/user/{id}", userId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "HOST"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.role").value("GUEST"));
        }

        @Test
        @DisplayName("Should return 200 OK with UserResponse when caller is a GUEST")
        void getById_WithGuestRole_ReturnsUserResponse() throws Exception {
            // Given
            when(userService.getProfile(eq(userId))).thenReturn(userResponse);

            // When/Then
            mockMvc.perform(get("/api/user/{id}", userId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "GUEST"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @DisplayName("Should return 401 when auth headers are missing")
        void getById_WithoutHeaders_Returns401() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/user/{id}", userId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when role is not allowed")
        void getById_WithWrongRole_Returns403() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/user/{id}", userId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 NOT FOUND when user does not exist")
        void getById_UserNotFound_Returns404() throws Exception {
            // Given
            when(userService.getProfile(eq(userId)))
                    .thenThrow(new UserNotFoundException("User does not exist"));

            // When/Then
            mockMvc.perform(get("/api/user/{id}", userId)
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .header("X-User-Role", "HOST"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("User Not Found"))
                    .andExpect(jsonPath("$.detail").value("User does not exist"));
        }
    }
}