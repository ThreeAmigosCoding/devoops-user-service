package com.devoops.user.grpc;

import com.devoops.user.entity.Role;
import com.devoops.user.entity.User;
import com.devoops.user.grpc.proto.GetUserSummaryRequest;
import com.devoops.user.grpc.proto.GetUserSummaryResponse;
import com.devoops.user.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserGrpcServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StreamObserver<GetUserSummaryResponse> responseObserver;

    @InjectMocks
    private UserGrpcService userGrpcService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = buildTestUser();
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

    @Nested
    @DisplayName("getUserSummary Tests")
    class GetUserSummaryTests {

        @Test
        @DisplayName("Should return user summary when user exists")
        void getUserSummary_WithExistingUser_ReturnsSummary() {
            // Given
            GetUserSummaryRequest request = GetUserSummaryRequest.newBuilder()
                    .setUserId(testUserId.toString())
                    .build();
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            userGrpcService.getUserSummary(request, responseObserver);

            // Then
            ArgumentCaptor<GetUserSummaryResponse> captor = ArgumentCaptor.forClass(GetUserSummaryResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();
            verify(responseObserver, never()).onError(any());

            GetUserSummaryResponse response = captor.getValue();
            assertThat(response.getFound()).isTrue();
            assertThat(response.getUserId()).isEqualTo(testUserId.toString());
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getFirstName()).isEqualTo("Test");
            assertThat(response.getLastName()).isEqualTo("User");
            assertThat(response.getRole()).isEqualTo("GUEST");
            assertThat(response.getIsDeleted()).isFalse();
        }

        @Test
        @DisplayName("Should return not found when user does not exist")
        void getUserSummary_WithNonExistingUser_ReturnsNotFound() {
            // Given
            UUID unknownId = UUID.randomUUID();
            GetUserSummaryRequest request = GetUserSummaryRequest.newBuilder()
                    .setUserId(unknownId.toString())
                    .build();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
            when(userRepository.findByIdIncludingDeleted(unknownId)).thenReturn(Optional.empty());

            // When
            userGrpcService.getUserSummary(request, responseObserver);

            // Then
            ArgumentCaptor<GetUserSummaryResponse> captor = ArgumentCaptor.forClass(GetUserSummaryResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();
            verify(responseObserver, never()).onError(any());

            GetUserSummaryResponse response = captor.getValue();
            assertThat(response.getFound()).isFalse();
            assertThat(response.getUserId()).isEmpty();
            assertThat(response.getEmail()).isEmpty();
        }

        @Test
        @DisplayName("Should return deleted user with isDeleted flag when user is soft-deleted")
        void getUserSummary_WithDeletedUser_ReturnsUserWithDeletedFlag() {
            // Given
            GetUserSummaryRequest request = GetUserSummaryRequest.newBuilder()
                    .setUserId(testUserId.toString())
                    .build();
            testUser.setDeleted(true);
            when(userRepository.findById(testUserId)).thenReturn(Optional.empty());
            when(userRepository.findByIdIncludingDeleted(testUserId)).thenReturn(Optional.of(testUser));

            // When
            userGrpcService.getUserSummary(request, responseObserver);

            // Then
            ArgumentCaptor<GetUserSummaryResponse> captor = ArgumentCaptor.forClass(GetUserSummaryResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();
            verify(responseObserver, never()).onError(any());

            GetUserSummaryResponse response = captor.getValue();
            assertThat(response.getFound()).isTrue();
            assertThat(response.getUserId()).isEqualTo(testUserId.toString());
            assertThat(response.getEmail()).isEqualTo("test@example.com");
            assertThat(response.getFirstName()).isEqualTo("Test");
            assertThat(response.getLastName()).isEqualTo("User");
            assertThat(response.getRole()).isEqualTo("GUEST");
            assertThat(response.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("Should return not found when user ID is invalid UUID")
        void getUserSummary_WithInvalidUUID_ReturnsNotFound() {
            // Given
            GetUserSummaryRequest request = GetUserSummaryRequest.newBuilder()
                    .setUserId("invalid-uuid")
                    .build();

            // When
            userGrpcService.getUserSummary(request, responseObserver);

            // Then
            ArgumentCaptor<GetUserSummaryResponse> captor = ArgumentCaptor.forClass(GetUserSummaryResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();
            verify(responseObserver, never()).onError(any());

            GetUserSummaryResponse response = captor.getValue();
            assertThat(response.getFound()).isFalse();

            // Repository should not be called for invalid UUID
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return not found when user ID is empty")
        void getUserSummary_WithEmptyUserId_ReturnsNotFound() {
            // Given
            GetUserSummaryRequest request = GetUserSummaryRequest.newBuilder()
                    .setUserId("")
                    .build();

            // When
            userGrpcService.getUserSummary(request, responseObserver);

            // Then
            ArgumentCaptor<GetUserSummaryResponse> captor = ArgumentCaptor.forClass(GetUserSummaryResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();

            GetUserSummaryResponse response = captor.getValue();
            assertThat(response.getFound()).isFalse();

            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should return correct role for HOST user")
        void getUserSummary_WithHostUser_ReturnsHostRole() {
            // Given
            testUser.setRole(Role.HOST);
            GetUserSummaryRequest request = GetUserSummaryRequest.newBuilder()
                    .setUserId(testUserId.toString())
                    .build();
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            // When
            userGrpcService.getUserSummary(request, responseObserver);

            // Then
            ArgumentCaptor<GetUserSummaryResponse> captor = ArgumentCaptor.forClass(GetUserSummaryResponse.class);
            verify(responseObserver).onNext(captor.capture());

            GetUserSummaryResponse response = captor.getValue();
            assertThat(response.getFound()).isTrue();
            assertThat(response.getRole()).isEqualTo("HOST");
        }

        @Test
        @DisplayName("Should handle null user ID gracefully")
        void getUserSummary_WithNullUserId_ReturnsNotFound() {
            // Given - protobuf returns empty string for null
            GetUserSummaryRequest request = GetUserSummaryRequest.newBuilder()
                    .build();

            // When
            userGrpcService.getUserSummary(request, responseObserver);

            // Then
            ArgumentCaptor<GetUserSummaryResponse> captor = ArgumentCaptor.forClass(GetUserSummaryResponse.class);
            verify(responseObserver).onNext(captor.capture());
            verify(responseObserver).onCompleted();

            GetUserSummaryResponse response = captor.getValue();
            assertThat(response.getFound()).isFalse();

            verify(userRepository, never()).findById(any());
        }
    }
}
