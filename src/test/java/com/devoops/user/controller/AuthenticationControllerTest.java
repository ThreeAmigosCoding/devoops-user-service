package com.devoops.user.controller;

import com.devoops.user.dto.request.LoginRequest;
import com.devoops.user.dto.request.RegisterRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.entity.Role;
import com.devoops.user.exception.GlobalExceptionHandler;
import com.devoops.user.exception.InvalidCredentialsException;
import com.devoops.user.exception.UserAlreadyExistsException;
import com.devoops.user.service.AuthenticationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UserResponse userResponse;
    private AuthenticationResponse authenticationResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        userResponse = new UserResponse(
                UUID.randomUUID(),
                "testuser",
                "test@example.com",
                "Test",
                "User",
                "Belgrade",
                Role.GUEST
        );

        authenticationResponse = new AuthenticationResponse(
                "jwt-token-here",
                86400000L,
                userResponse
        );
    }

    @Nested
    @DisplayName("POST /api/user/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Should return 201 CREATED with token when registration is successful")
        void register_Success_Returns201() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "testuser",
                    "password123",
                    "test@example.com",
                    "Test",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenReturn(authenticationResponse);

            // When/Then
            mockMvc.perform(post("/api/user/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken").value("jwt-token-here"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(86400000))
                    .andExpect(jsonPath("$.user.username").value("testuser"))
                    .andExpect(jsonPath("$.user.email").value("test@example.com"))
                    .andExpect(jsonPath("$.user.role").value("GUEST"));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when username already exists")
        void register_UsernameExists_Returns409() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "existinguser",
                    "password123",
                    "test@example.com",
                    "Test",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("Username already taken: existinguser"));

            // When/Then
            mockMvc.perform(post("/api/user/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("User Already Exists"))
                    .andExpect(jsonPath("$.detail").value("Username already taken: existinguser"));
        }

        @Test
        @DisplayName("Should return 409 CONFLICT when email already exists")
        void register_EmailExists_Returns409() throws Exception {
            // Given
            RegisterRequest request = new RegisterRequest(
                    "newuser",
                    "password123",
                    "existing@example.com",
                    "Test",
                    "User",
                    "Belgrade",
                    Role.GUEST
            );

            when(authenticationService.register(any(RegisterRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("Email already registered: existing@example.com"));

            // When/Then
            mockMvc.perform(post("/api/user/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Email already registered: existing@example.com"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when request body is invalid")
        void register_InvalidRequest_Returns400() throws Exception {
            // Given - Empty request
            String invalidRequest = "{}";

            // When/Then
            mockMvc.perform(post("/api/user/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/user/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return 200 OK with token when login is successful")
        void login_Success_Returns200() throws Exception {
            // Given
            LoginRequest request = new LoginRequest("testuser", "password123");

            when(authenticationService.login(any(LoginRequest.class)))
                    .thenReturn(authenticationResponse);

            // When/Then
            mockMvc.perform(post("/api/user/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.accessToken").value("jwt-token-here"))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.user.username").value("testuser"));
        }

        @Test
        @DisplayName("Should return 401 UNAUTHORIZED when credentials are invalid")
        void login_InvalidCredentials_Returns401() throws Exception {
            // Given
            LoginRequest request = new LoginRequest("testuser", "wrongpassword");

            when(authenticationService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException("Invalid username/email or password"));

            // When/Then
            mockMvc.perform(post("/api/user/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Invalid Credentials"))
                    .andExpect(jsonPath("$.detail").value("Invalid username/email or password"));
        }

        @Test
        @DisplayName("Should return 400 BAD REQUEST when request body is invalid")
        void login_InvalidRequest_Returns400() throws Exception {
            // Given - Empty credentials
            String invalidRequest = """
                    {
                        "usernameOrEmail": "",
                        "password": ""
                    }
                    """;

            // When/Then
            mockMvc.perform(post("/api/user/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }
}
