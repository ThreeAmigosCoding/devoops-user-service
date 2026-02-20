package com.devoops.user.grpc;

import com.devoops.user.grpc.proto.accommodation.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccommodationGrpcClientTest {

    @Mock
    private AccommodationInternalServiceGrpc.AccommodationInternalServiceBlockingStub accommodationStub;

    private AccommodationGrpcClient accommodationGrpcClient;

    @BeforeEach
    void setUp() throws Exception {
        accommodationGrpcClient = new AccommodationGrpcClient();
        // Use reflection to inject the mock stub
        Field stubField = AccommodationGrpcClient.class.getDeclaredField("accommodationStub");
        stubField.setAccessible(true);
        stubField.set(accommodationGrpcClient, accommodationStub);
    }

    @Nested
    @DisplayName("deleteAccommodationsByHost Tests")
    class DeleteAccommodationsByHostTests {

        @Test
        @DisplayName("Should return success when accommodations are deleted successfully")
        void deleteAccommodationsByHost_Success_ReturnsSuccessResult() {
            // Given
            UUID hostId = UUID.randomUUID();
            DeleteByHostResponse response = DeleteByHostResponse.newBuilder()
                    .setSuccess(true)
                    .setDeletedCount(5)
                    .setErrorMessage("")
                    .build();
            when(accommodationStub.deleteAccommodationsByHost(any(DeleteByHostRequest.class)))
                    .thenReturn(response);

            // When
            CascadeDeleteResult result = accommodationGrpcClient.deleteAccommodationsByHost(hostId);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.deletedCount()).isEqualTo(5);
            assertThat(result.errorMessage()).isEmpty();
        }

        @Test
        @DisplayName("Should return success with zero count when host has no accommodations")
        void deleteAccommodationsByHost_NoAccommodations_ReturnsSuccessWithZeroCount() {
            // Given
            UUID hostId = UUID.randomUUID();
            DeleteByHostResponse response = DeleteByHostResponse.newBuilder()
                    .setSuccess(true)
                    .setDeletedCount(0)
                    .setErrorMessage("")
                    .build();
            when(accommodationStub.deleteAccommodationsByHost(any(DeleteByHostRequest.class)))
                    .thenReturn(response);

            // When
            CascadeDeleteResult result = accommodationGrpcClient.deleteAccommodationsByHost(hostId);

            // Then
            assertThat(result.success()).isTrue();
            assertThat(result.deletedCount()).isZero();
            assertThat(result.errorMessage()).isEmpty();
        }

        @Test
        @DisplayName("Should return failure when deletion fails")
        void deleteAccommodationsByHost_Failure_ReturnsFailureResult() {
            // Given
            UUID hostId = UUID.randomUUID();
            DeleteByHostResponse response = DeleteByHostResponse.newBuilder()
                    .setSuccess(false)
                    .setDeletedCount(0)
                    .setErrorMessage("Database constraint violation")
                    .build();
            when(accommodationStub.deleteAccommodationsByHost(any(DeleteByHostRequest.class)))
                    .thenReturn(response);

            // When
            CascadeDeleteResult result = accommodationGrpcClient.deleteAccommodationsByHost(hostId);

            // Then
            assertThat(result.success()).isFalse();
            assertThat(result.deletedCount()).isZero();
            assertThat(result.errorMessage()).isEqualTo("Database constraint violation");
        }

        @Test
        @DisplayName("Should throw RuntimeException when gRPC call fails")
        void deleteAccommodationsByHost_GrpcError_ThrowsRuntimeException() {
            // Given
            UUID hostId = UUID.randomUUID();
            when(accommodationStub.deleteAccommodationsByHost(any(DeleteByHostRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

            // When/Then
            assertThatThrownBy(() -> accommodationGrpcClient.deleteAccommodationsByHost(hostId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to delete host accommodations");
        }
    }
}
