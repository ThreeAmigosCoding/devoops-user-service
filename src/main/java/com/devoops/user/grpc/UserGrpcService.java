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

        // First try to find active (non-deleted) user
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            log.debug("Found active user: {} {} ({})", user.getFirstName(), user.getLastName(), user.getRole());
            return buildUserResponse(user, false);
        }

        // If not found, check if user exists but is deleted
        Optional<User> deletedUserOpt = userRepository.findByIdIncludingDeleted(userId);
        if (deletedUserOpt.isPresent()) {
            User user = deletedUserOpt.get();
            log.debug("Found deleted user: {} {} ({})", user.getFirstName(), user.getLastName(), user.getRole());
            return buildUserResponse(user, true);
        }

        log.debug("User not found: {}", userId);
        return buildNotFoundResponse();
    }

    private GetUserSummaryResponse buildUserResponse(User user, boolean isDeleted) {
        return GetUserSummaryResponse.newBuilder()
                .setFound(true)
                .setUserId(user.getId().toString())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setRole(user.getRole().name())
                .setIsDeleted(isDeleted)
                .build();
    }

    private GetUserSummaryResponse buildNotFoundResponse() {
        return GetUserSummaryResponse.newBuilder()
                .setFound(false)
                .build();
    }
}
