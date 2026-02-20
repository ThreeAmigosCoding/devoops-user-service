package com.devoops.user.grpc;

import com.devoops.user.grpc.proto.reservation.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class ReservationGrpcClient {

    @GrpcClient("reservation-service")
    private ReservationInternalServiceGrpc.ReservationInternalServiceBlockingStub reservationStub;

    /**
     * Check if a guest can be deleted (has no active reservations).
     */
    public DeletionCheckResult checkGuestCanBeDeleted(UUID guestId) {
        log.debug("Checking if guest {} can be deleted", guestId);

        try {
            CheckGuestDeletionRequest request = CheckGuestDeletionRequest.newBuilder()
                    .setGuestId(guestId.toString())
                    .build();

            CheckDeletionResponse response = reservationStub.checkGuestCanBeDeleted(request);

            return new DeletionCheckResult(
                    response.getCanBeDeleted(),
                    response.getReason(),
                    response.getActiveReservationCount()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking if guest {} can be deleted: {}", guestId, e.getStatus(), e);
            throw new RuntimeException("Failed to check guest deletion eligibility", e);
        }
    }

    /**
     * Check if a host can be deleted (has no active reservations on their accommodations).
     */
    public DeletionCheckResult checkHostCanBeDeleted(UUID hostId) {
        log.debug("Checking if host {} can be deleted", hostId);

        try {
            CheckHostDeletionRequest request = CheckHostDeletionRequest.newBuilder()
                    .setHostId(hostId.toString())
                    .build();

            CheckDeletionResponse response = reservationStub.checkHostCanBeDeleted(request);

            return new DeletionCheckResult(
                    response.getCanBeDeleted(),
                    response.getReason(),
                    response.getActiveReservationCount()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error checking if host {} can be deleted: {}", hostId, e.getStatus(), e);
            throw new RuntimeException("Failed to check host deletion eligibility", e);
        }
    }
}
