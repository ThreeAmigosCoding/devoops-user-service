package com.devoops.user.grpc;

import com.devoops.user.grpc.proto.reservation.*;
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
class ReservationGrpcClientTest {

    @Mock
    private ReservationInternalServiceGrpc.ReservationInternalServiceBlockingStub reservationStub;

    private ReservationGrpcClient reservationGrpcClient;

    @BeforeEach
    void setUp() throws Exception {
        reservationGrpcClient = new ReservationGrpcClient();
        // Use reflection to inject the mock stub
        Field stubField = ReservationGrpcClient.class.getDeclaredField("reservationStub");
        stubField.setAccessible(true);
        stubField.set(reservationGrpcClient, reservationStub);
    }

    @Nested
    @DisplayName("checkGuestCanBeDeleted Tests")
    class CheckGuestCanBeDeletedTests {

        @Test
        @DisplayName("Should return can be deleted when guest has no active reservations")
        void checkGuestCanBeDeleted_NoActiveReservations_ReturnsCanBeDeleted() {
            // Given
            UUID guestId = UUID.randomUUID();
            CheckDeletionResponse response = CheckDeletionResponse.newBuilder()
                    .setCanBeDeleted(true)
                    .setReason("")
                    .setActiveReservationCount(0)
                    .build();
            when(reservationStub.checkGuestCanBeDeleted(any(CheckGuestDeletionRequest.class)))
                    .thenReturn(response);

            // When
            DeletionCheckResult result = reservationGrpcClient.checkGuestCanBeDeleted(guestId);

            // Then
            assertThat(result.canBeDeleted()).isTrue();
            assertThat(result.reason()).isEmpty();
            assertThat(result.activeReservationCount()).isZero();
        }

        @Test
        @DisplayName("Should return cannot be deleted when guest has active reservations")
        void checkGuestCanBeDeleted_HasActiveReservations_ReturnsCannotBeDeleted() {
            // Given
            UUID guestId = UUID.randomUUID();
            CheckDeletionResponse response = CheckDeletionResponse.newBuilder()
                    .setCanBeDeleted(false)
                    .setReason("Guest has 3 active reservations")
                    .setActiveReservationCount(3)
                    .build();
            when(reservationStub.checkGuestCanBeDeleted(any(CheckGuestDeletionRequest.class)))
                    .thenReturn(response);

            // When
            DeletionCheckResult result = reservationGrpcClient.checkGuestCanBeDeleted(guestId);

            // Then
            assertThat(result.canBeDeleted()).isFalse();
            assertThat(result.reason()).isEqualTo("Guest has 3 active reservations");
            assertThat(result.activeReservationCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should throw RuntimeException when gRPC call fails")
        void checkGuestCanBeDeleted_GrpcError_ThrowsRuntimeException() {
            // Given
            UUID guestId = UUID.randomUUID();
            when(reservationStub.checkGuestCanBeDeleted(any(CheckGuestDeletionRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.UNAVAILABLE));

            // When/Then
            assertThatThrownBy(() -> reservationGrpcClient.checkGuestCanBeDeleted(guestId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to check guest deletion eligibility");
        }
    }

    @Nested
    @DisplayName("checkHostCanBeDeleted Tests")
    class CheckHostCanBeDeletedTests {

        @Test
        @DisplayName("Should return can be deleted when host has no active reservations")
        void checkHostCanBeDeleted_NoActiveReservations_ReturnsCanBeDeleted() {
            // Given
            UUID hostId = UUID.randomUUID();
            CheckDeletionResponse response = CheckDeletionResponse.newBuilder()
                    .setCanBeDeleted(true)
                    .setReason("")
                    .setActiveReservationCount(0)
                    .build();
            when(reservationStub.checkHostCanBeDeleted(any(CheckHostDeletionRequest.class)))
                    .thenReturn(response);

            // When
            DeletionCheckResult result = reservationGrpcClient.checkHostCanBeDeleted(hostId);

            // Then
            assertThat(result.canBeDeleted()).isTrue();
            assertThat(result.reason()).isEmpty();
            assertThat(result.activeReservationCount()).isZero();
        }

        @Test
        @DisplayName("Should return cannot be deleted when host has active reservations")
        void checkHostCanBeDeleted_HasActiveReservations_ReturnsCannotBeDeleted() {
            // Given
            UUID hostId = UUID.randomUUID();
            CheckDeletionResponse response = CheckDeletionResponse.newBuilder()
                    .setCanBeDeleted(false)
                    .setReason("Host has 5 active reservations on their accommodations")
                    .setActiveReservationCount(5)
                    .build();
            when(reservationStub.checkHostCanBeDeleted(any(CheckHostDeletionRequest.class)))
                    .thenReturn(response);

            // When
            DeletionCheckResult result = reservationGrpcClient.checkHostCanBeDeleted(hostId);

            // Then
            assertThat(result.canBeDeleted()).isFalse();
            assertThat(result.reason()).isEqualTo("Host has 5 active reservations on their accommodations");
            assertThat(result.activeReservationCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw RuntimeException when gRPC call fails")
        void checkHostCanBeDeleted_GrpcError_ThrowsRuntimeException() {
            // Given
            UUID hostId = UUID.randomUUID();
            when(reservationStub.checkHostCanBeDeleted(any(CheckHostDeletionRequest.class)))
                    .thenThrow(new StatusRuntimeException(Status.INTERNAL));

            // When/Then
            assertThatThrownBy(() -> reservationGrpcClient.checkHostCanBeDeleted(hostId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to check host deletion eligibility");
        }
    }
}
