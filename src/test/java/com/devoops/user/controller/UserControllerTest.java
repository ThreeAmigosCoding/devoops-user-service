package com.devoops.user.controller;

import com.devoops.user.config.RoleAuthorizationInterceptor;
import com.devoops.user.config.UserContextResolver;
import com.devoops.user.dto.request.ChangePasswordRequest;
import com.devoops.user.dto.request.UpdateUserRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.entity.Role;
import com.devoops.user.exception.GlobalExceptionHandler;
import com.devoops.user.exception.InvalidCredentialsException;
import com.devoops.user.exception.UserAlreadyExistsException;
import com.devoops.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID userId;
    private UserResponse userResponse;
    private AuthenticationResponse authenticationResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new UserContextResolver())
                .addInterceptors(new RoleAuthorizationInterceptor())
                .build();
        objectMapper = new ObjectMapper();

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

        authenticationResponse = new AuthenticationResponse(
                "jwt-token-here",
                86400000L,
                userResponse
        );
    }

    @Nested
    @DisplayName("GET /api/user/me — getProfile")
    class GetProfileTests {

        @Test
        @DisplayName("Should return 200 OK with UserResponse when auth headers are valid")
        void getProfile_WithValidHeaders_ReturnsUserResponse() throws Exception {
            // Given
            when(userService.getProfile(eq(userId))).thenReturn(userResponse);

            // When/Then
            mockMvc.perform(get("/api/user/me")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.role").value("GUEST"));
        }

        @Test
        @DisplayName("Should return 401 when auth headers are missing")
        void getProfile_WithoutHeaders_Returns401() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/user/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when role is not allowed")
        void getProfile_WithWrongRole_Returns403() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/user/me")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/user/me — updateProfile")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should return 200 OK with AuthenticationResponse when request is valid")
        void updateProfile_WithValidRequest_ReturnsAuthenticationResponse() throws Exception {
            // Given
            UpdateUserRequest request = new UpdateUserRequest(
                    "newusername", "new@example.com", "New", "Name", "New City"
            );
            when(userService.updateProfile(eq(userId), any(UpdateUserRequest.class)))
                    .thenReturn(authenticationResponse);

            // When/Then
            mockMvc.perform(put("/api/user/me")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken").value("jwt-token-here"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.username").value("testuser"));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when username is already taken")
        void updateProfile_WithUsernameTaken_Returns409() throws Exception {
            // Given
            UpdateUserRequest request = new UpdateUserRequest(
                    "takenuser", null, null, null, null
            );
            when(userService.updateProfile(eq(userId), any(UpdateUserRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("Username already taken"));

            // When/Then
            mockMvc.perform(put("/api/user/me")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("User Already Exists"))
                    .andExpect(jsonPath("$.detail").value("Username already taken"));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when email is already taken")
        void updateProfile_WithEmailTaken_Returns409() throws Exception {
            // Given
            UpdateUserRequest request = new UpdateUserRequest(
                    null, "taken@example.com", null, null, null
            );
            when(userService.updateProfile(eq(userId), any(UpdateUserRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("Email already taken"));

            // When/Then
            mockMvc.perform(put("/api/user/me")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Email already taken"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when email is invalid")
        void updateProfile_WithInvalidEmail_Returns400() throws Exception {
            // Given
            String invalidRequest = """
                    {
                        "email": "not-an-email"
                    }
                    """;

            // When/Then
            mockMvc.perform(put("/api/user/me")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when username is too short")
        void updateProfile_WithUsernameTooShort_Returns400() throws Exception {
            // Given
            String invalidRequest = """
                    {
                        "username": "ab"
                    }
                    """;

            // When/Then
            mockMvc.perform(put("/api/user/me")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/user/me/password — changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should return 204 NO CONTENT when password change is successful")
        void changePassword_WithValidRequest_Returns204() throws Exception {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass1", "newPassword123");

            // When/Then
            mockMvc.perform(put("/api/user/me/password")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when current password is incorrect")
        void changePassword_WithInvalidCurrentPassword_Returns401() throws Exception {
            // Given
            ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPassword123");
            doThrow(new InvalidCredentialsException("Current password is incorrect"))
                    .when(userService).changePassword(eq(userId), any(ChangePasswordRequest.class));

            // When/Then
            mockMvc.perform(put("/api/user/me/password")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when current password is blank")
        void changePassword_WithBlankCurrentPassword_Returns400() throws Exception {
            // Given
            String invalidRequest = """
                    {
                        "currentPassword": "",
                        "newPassword": "newPassword123"
                    }
                    """;

            // When/Then
            mockMvc.perform(put("/api/user/me/password")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when new password is too short")
        void changePassword_WithShortNewPassword_Returns400() throws Exception {
            // Given
            String invalidRequest = """
                    {
                        "currentPassword": "current123",
                        "newPassword": "short"
                    }
                    """;

            // When/Then
            mockMvc.perform(put("/api/user/me/password")
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", "GUEST")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }
}
