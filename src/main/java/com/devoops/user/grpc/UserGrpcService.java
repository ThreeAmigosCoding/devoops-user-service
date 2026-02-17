package com.devoops.user.grpc;

import com.devoops.user.entity.User;
import com.devoops.user.grpc.proto.GetUserSummaryRequest;
import com.devoops.user.grpc.proto.GetUserSummaryResponse;
import com.devoops.user.grpc.proto.UserInternalServiceGrpc;
import com.devoops.user.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService extends UserInternalServiceGrpc.UserInternalServiceImplBase {

    private final UserRepository userRepository;

    @Override
    public void getUserSummary(
            GetUserSummaryRequest request,
            StreamObserver<GetUserSummaryResponse> responseObserver) {

        log.debug("Received GetUserSummary request for userId: {}", request.getUserId());

        GetUserSummaryResponse response = processRequest(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private GetUserSummaryResponse processRequest(GetUserSummaryRequest request) {
        UUID userId;
        try {
            userId = UUID.fromString(request.getUserId());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", request.getUserId());
            return buildNotFoundResponse();
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.debug("User not found: {}", userId);
            return buildNotFoundResponse();
        }

        User user = userOpt.get();
        log.debug("Found user: {} {} ({})", user.getFirstName(), user.getLastName(), user.getRole());

        return GetUserSummaryResponse.newBuilder()
                .setFound(true)
                .setUserId(user.getId().toString())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setRole(user.getRole().name())
                .build();
    }

    private GetUserSummaryResponse buildNotFoundResponse() {
        return GetUserSummaryResponse.newBuilder()
                .setFound(false)
                .build();
    }
}
