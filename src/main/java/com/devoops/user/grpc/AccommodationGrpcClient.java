package com.devoops.user.grpc;

import com.devoops.user.grpc.proto.accommodation.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class AccommodationGrpcClient {

    @GrpcClient("accommodation-service")
    private AccommodationInternalServiceGrpc.AccommodationInternalServiceBlockingStub accommodationStub;

    /**
     * Delete all accommodations for a host (cascade deletion).
     */
    public CascadeDeleteResult deleteAccommodationsByHost(UUID hostId) {
        log.debug("Deleting all accommodations for host {}", hostId);

        try {
            DeleteByHostRequest request = DeleteByHostRequest.newBuilder()
                    .setHostId(hostId.toString())
                    .build();

            DeleteByHostResponse response = accommodationStub.deleteAccommodationsByHost(request);

            if (response.getSuccess()) {
                log.info("Successfully deleted {} accommodations for host {}", response.getDeletedCount(), hostId);
            } else {
                log.error("Failed to delete accommodations for host {}: {}", hostId, response.getErrorMessage());
            }

            return new CascadeDeleteResult(
                    response.getSuccess(),
                    response.getDeletedCount(),
                    response.getErrorMessage()
            );
        } catch (StatusRuntimeException e) {
            log.error("gRPC error deleting accommodations for host {}: {}", hostId, e.getStatus(), e);
            throw new RuntimeException("Failed to delete host accommodations", e);
        }
    }
}
