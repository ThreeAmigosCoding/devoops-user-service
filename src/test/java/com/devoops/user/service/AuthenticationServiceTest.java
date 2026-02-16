package com.devoops.user.service;

import com.devoops.user.dto.request.LoginRequest;
import com.devoops.user.dto.request.RegisterRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.entity.Role;
import com.devoops.user.entity.User;
import com.devoops.user.exception.InvalidCredentialsException;
import com.devoops.user.exception.UserAlreadyExistsException;
import com.devoops.user.mapper.UserMapper;
import com.devoops.user.repository.UserRepository;
import com.devoops.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventPublisherService userEventPublisherService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest(
                "testuser",
                "password123",
                "test@example.com",
                "Test",
                "User",
                "Belgrade",
                Role.GUEST
        );

        loginRequest = new LoginRequest("testuser", "password123");

        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .residence("Belgrade")
                .role(Role.GUEST)
                .build();

        userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getResidence(),
                user.getRole()
        );
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        void register_WithValidData_ReturnsAuthenticationResponse() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userMapper.toEntity(any(RegisterRequest.class))).thenReturn(user);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);
            when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

            // When
            AuthenticationResponse response = authenticationService.register(registerRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("jwt-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.user().username()).isEqualTo("testuser");
            assertThat(response.user().role()).isEqualTo(Role.GUEST);

            verify(userRepository).existsByUsername("testuser");
            verify(userRepository).existsByEmail("test@example.com");
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void register_WithExistingUsername_ThrowsUserAlreadyExistsException() {
            // Given
            when(userRepository.existsByUsername("testuser")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already taken");

            verify(userRepository).existsByUsername("testuser");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void register_WithExistingEmail_ThrowsUserAlreadyExistsException() {
            // Given
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authenticationService.register(registerRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully with username")
        void login_WithValidCredentials_ReturnsAuthenticationResponse() {
            // Given
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken(user, null));
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);
            when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

            // When
            AuthenticationResponse response = authenticationService.login(loginRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("jwt-token");
            assertThat(response.user().username()).isEqualTo("testuser");

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should login user successfully with email")
        void login_WithEmail_ReturnsAuthenticationResponse() {
            // Given
            LoginRequest emailLoginRequest = new LoginRequest("test@example.com", "password123");
            when(userRepository.findByUsernameOrEmail("test@example.com", "test@example.com"))
                    .thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken(user, null));
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);
            when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

            // When
            AuthenticationResponse response = authenticationService.login(emailLoginRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void login_WithNonExistentUser_ThrowsInvalidCredentialsException() {
            // Given
            when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid username/email or password");

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Should throw exception when password is incorrect")
        void login_WithWrongPassword_ThrowsInvalidCredentialsException() {
            // Given
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When/Then
            assertThatThrownBy(() -> authenticationService.login(loginRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid username/email or password");
        }
    }
}
