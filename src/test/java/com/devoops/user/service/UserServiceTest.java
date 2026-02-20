package com.devoops.user.service;

import com.devoops.user.dto.request.ChangePasswordRequest;
import com.devoops.user.dto.request.UpdateUserRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.entity.Role;
import com.devoops.user.entity.User;
import com.devoops.user.exception.AccountDeletionException;
import com.devoops.user.exception.InvalidPasswordException;
import com.devoops.user.exception.UserAlreadyExistsException;
import com.devoops.user.exception.UserNotFoundException;
import com.devoops.user.grpc.AccommodationGrpcClient;
import com.devoops.user.grpc.CascadeDeleteResult;
import com.devoops.user.grpc.DeletionCheckResult;
import com.devoops.user.grpc.ReservationGrpcClient;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private ReservationGrpcClient reservationGrpcClient;

    @Mock
    private AccommodationGrpcClient accommodationGrpcClient;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UUID testUserId;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = buildTestUser();

        userResponse = new UserResponse(
                testUserId,
                "testuser",
                "test@example.com",
                "Test",
                "User",
                "Test City",
                Role.GUEST
        );
    }

    private User buildTestUser() {
        return User.builder()
                .id(testUserId)
                .username("testuser")
                .password("encoded_password")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .residence("Test City")
                .role(Role.GUEST)
                .build();
    }

    private User buildTestHost() {
        return User.builder()
                .id(testUserId)
                .username("testhost")
                .password("encoded_password")
                .email("host@example.com")
                .firstName("Test")
                .lastName("Host")
                .residence("Test City")
                .role(Role.HOST)
                .build();
    }

    @Nested
    @DisplayName("getProfile Tests")
    class GetProfileTests {

        @Test
        @DisplayName("Should return UserResponse when user exists")
        void getProfile_WithValidUserId_ReturnsUserResponse() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userMapper.toUserResponse(testUser)).thenReturn(userResponse);

            // When
            UserResponse result = userService.getProfile(testUserId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.username()).isEqualTo("testuser");
            assertThat(result.email()).isEqualTo("test@example.com");
            verify(userRepository).findById(testUserId);
            verify(userMapper).toUserResponse(testUser);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void getProfile_WithInvalidUserId_ThrowsUserNotFoundException() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.getProfile(unknownId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User does not exist");
        }
    }

    @Nested
    @DisplayName("updateProfile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update username and return new token when username is new")
        void updateProfile_WithNewUsername_UpdatesAndReturnsToken() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            UpdateUserRequest request = new UpdateUserRequest("newuser", null, null, null, null);
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("new-token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);
            when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

            // When
            AuthenticationResponse result = userService.updateProfile(testUserId, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo("new-token");
            verify(userRepository).existsByUsername("newuser");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when username is taken")
        void updateProfile_WithExistingUsername_ThrowsUserAlreadyExistsException() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            UpdateUserRequest request = new UpdateUserRequest("takenuser", null, null, null, null);
            when(userRepository.existsByUsername("takenuser")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.updateProfile(testUserId, request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update email and return new token when email is new")
        void updateProfile_WithNewEmail_UpdatesAndReturnsToken() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            UpdateUserRequest request = new UpdateUserRequest(null, "new@example.com", null, null, null);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("new-token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);
            when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

            // When
            AuthenticationResponse result = userService.updateProfile(testUserId, request);

            // Then
            assertThat(result).isNotNull();
            verify(userRepository).existsByEmail("new@example.com");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when email is taken")
        void updateProfile_WithExistingEmail_ThrowsUserAlreadyExistsException() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            UpdateUserRequest request = new UpdateUserRequest(null, "taken@example.com", null, null, null);
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.updateProfile(testUserId, request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip username check when username is the same as current")
        void updateProfile_WithSameUsername_SkipsUsernameCheck() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            UpdateUserRequest request = new UpdateUserRequest("testuser", null, null, null, null);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);
            when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

            // When
            userService.updateProfile(testUserId, request);

            // Then
            verify(userRepository, never()).existsByUsername(anyString());
        }

        @Test
        @DisplayName("Should only update non-null fields")
        void updateProfile_WithNullFields_OnlyUpdatesNonNullFields() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            UpdateUserRequest request = new UpdateUserRequest(null, null, "UpdatedFirst", null, null);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("token");
            when(jwtService.getExpirationTime()).thenReturn(86400000L);
            when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

            // When
            userService.updateProfile(testUserId, request);

            // Then
            assertThat(testUser.getUsername()).isEqualTo("testuser");
            assertThat(testUser.getEmail()).isEqualTo("test@example.com");
            assertThat(testUser.getFirstName()).isEqualTo("UpdatedFirst");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void updateProfile_WithNonExistentUser_ThrowsUserNotFoundException() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
            UpdateUserRequest request = new UpdateUserRequest("newuser", null, null, null, null);

            // When/Then
            assertThatThrownBy(() -> userService.updateProfile(unknownId, request))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User does not exist");
        }
    }

    @Nested
    @DisplayName("changePassword Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should encode and save new password when current password matches")
        void changePassword_WithValidPassword_EncodesAndSaves() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            ChangePasswordRequest request = new ChangePasswordRequest("current", "newpassword123");
            when(passwordEncoder.matches("current", "encoded_password")).thenReturn(true);
            when(passwordEncoder.encode("newpassword123")).thenReturn("new_encoded");

            // When
            userService.changePassword(testUserId, request);

            // Then
            assertThat(testUser.getPassword()).isEqualTo("new_encoded");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should throw InvalidPasswordException when current password is wrong")
        void changePassword_WithIncorrectCurrentPassword_ThrowsInvalidPasswordException() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            ChangePasswordRequest request = new ChangePasswordRequest("wrong", "newpassword123");
            when(passwordEncoder.matches("wrong", "encoded_password")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(testUserId, request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("Current password is incorrect");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void changePassword_WithNonExistentUser_ThrowsUserNotFoundException() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
            ChangePasswordRequest request = new ChangePasswordRequest("current", "newpassword123");

            // When/Then
            assertThatThrownBy(() -> userService.changePassword(unknownId, request))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User does not exist");
        }
    }

    @Nested
    @DisplayName("deleteAccount Tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should delete guest account when no active reservations exist")
        void deleteAccount_GuestWithNoReservations_DeletesSuccessfully() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(reservationGrpcClient.checkGuestCanBeDeleted(testUserId))
                    .thenReturn(new DeletionCheckResult(true, "", 0));

            // When
            userService.deleteAccount(testUserId);

            // Then
            assertThat(testUser.isDeleted()).isTrue();
            verify(userRepository).save(testUser);
            verify(reservationGrpcClient).checkGuestCanBeDeleted(testUserId);
            verify(accommodationGrpcClient, never()).deleteAccommodationsByHost(any());
        }

        @Test
        @DisplayName("Should throw AccountDeletionException when guest has active reservations")
        void deleteAccount_GuestWithActiveReservations_ThrowsAccountDeletionException() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(reservationGrpcClient.checkGuestCanBeDeleted(testUserId))
                    .thenReturn(new DeletionCheckResult(false, "Guest has 2 active reservations", 2));

            // When/Then
            assertThatThrownBy(() -> userService.deleteAccount(testUserId))
                    .isInstanceOf(AccountDeletionException.class)
                    .hasMessageContaining("2 active reservation(s)");

            assertThat(testUser.isDeleted()).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should delete host account and cascade delete accommodations when no active reservations")
        void deleteAccount_HostWithNoReservations_DeletesWithCascade() {
            // Given
            User hostUser = buildTestHost();
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(hostUser));
            when(reservationGrpcClient.checkHostCanBeDeleted(testUserId))
                    .thenReturn(new DeletionCheckResult(true, "", 0));
            when(accommodationGrpcClient.deleteAccommodationsByHost(testUserId))
                    .thenReturn(new CascadeDeleteResult(true, 3, ""));

            // When
            userService.deleteAccount(testUserId);

            // Then
            assertThat(hostUser.isDeleted()).isTrue();
            verify(userRepository).save(hostUser);
            verify(reservationGrpcClient).checkHostCanBeDeleted(testUserId);
            verify(accommodationGrpcClient).deleteAccommodationsByHost(testUserId);
        }

        @Test
        @DisplayName("Should throw AccountDeletionException when host has active reservations on accommodations")
        void deleteAccount_HostWithActiveReservations_ThrowsAccountDeletionException() {
            // Given
            User hostUser = buildTestHost();
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(hostUser));
            when(reservationGrpcClient.checkHostCanBeDeleted(testUserId))
                    .thenReturn(new DeletionCheckResult(false, "Host has 5 active reservations", 5));

            // When/Then
            assertThatThrownBy(() -> userService.deleteAccount(testUserId))
                    .isInstanceOf(AccountDeletionException.class)
                    .hasMessageContaining("5 active reservation(s)")
                    .hasMessageContaining("your accommodations");

            assertThat(hostUser.isDeleted()).isFalse();
            verify(userRepository, never()).save(any());
            verify(accommodationGrpcClient, never()).deleteAccommodationsByHost(any());
        }

        @Test
        @DisplayName("Should throw RuntimeException when cascade delete fails")
        void deleteAccount_HostCascadeDeleteFails_ThrowsRuntimeException() {
            // Given
            User hostUser = buildTestHost();
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(hostUser));
            when(reservationGrpcClient.checkHostCanBeDeleted(testUserId))
                    .thenReturn(new DeletionCheckResult(true, "", 0));
            when(accommodationGrpcClient.deleteAccommodationsByHost(testUserId))
                    .thenReturn(new CascadeDeleteResult(false, 0, "Database error"));

            // When/Then
            assertThatThrownBy(() -> userService.deleteAccount(testUserId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to delete accommodations")
                    .hasMessageContaining("Database error");

            assertThat(hostUser.isDeleted()).isFalse();
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void deleteAccount_WithNonExistentUser_ThrowsUserNotFoundException() {
            // Given
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.deleteAccount(unknownId))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User does not exist");

            verify(reservationGrpcClient, never()).checkGuestCanBeDeleted(any());
            verify(reservationGrpcClient, never()).checkHostCanBeDeleted(any());
        }

        @Test
        @DisplayName("Should not call host deletion check for guest user")
        void deleteAccount_GuestUser_OnlyCallsGuestCheck() {
            // Given
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(reservationGrpcClient.checkGuestCanBeDeleted(testUserId))
                    .thenReturn(new DeletionCheckResult(true, "", 0));

            // When
            userService.deleteAccount(testUserId);

            // Then
            verify(reservationGrpcClient).checkGuestCanBeDeleted(testUserId);
            verify(reservationGrpcClient, never()).checkHostCanBeDeleted(any());
        }

        @Test
        @DisplayName("Should not call guest deletion check for host user")
        void deleteAccount_HostUser_OnlyCallsHostCheck() {
            // Given
            User hostUser = buildTestHost();
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(hostUser));
            when(reservationGrpcClient.checkHostCanBeDeleted(testUserId))
                    .thenReturn(new DeletionCheckResult(true, "", 0));
            when(accommodationGrpcClient.deleteAccommodationsByHost(testUserId))
                    .thenReturn(new CascadeDeleteResult(true, 0, ""));

            // When
            userService.deleteAccount(testUserId);

            // Then
            verify(reservationGrpcClient).checkHostCanBeDeleted(testUserId);
            verify(reservationGrpcClient, never()).checkGuestCanBeDeleted(any());
        }
    }
}
