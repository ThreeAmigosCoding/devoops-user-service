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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ReservationGrpcClient reservationGrpcClient;
    private final AccommodationGrpcClient accommodationGrpcClient;

    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist"));
        return userMapper.toUserResponse(user);
    }

    public AuthenticationResponse updateProfile(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist"));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new UserAlreadyExistsException("Username already taken");
            }
            user.setUsername(request.username());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new UserAlreadyExistsException("Email already taken");
            }
            user.setEmail(request.email());
        }

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.residence() != null) user.setResidence(request.residence());

        User saved = userRepository.save(user);
        return new AuthenticationResponse(jwtService.generateToken(saved), jwtService.getExpirationTime(), userMapper.toUserResponse(saved));
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /**
     * Delete a user account.
     * <p>
     * Guests can only delete their account if they have no active reservations
     * (PENDING or APPROVED with endDate >= today).
     * <p>
     * Hosts can only delete their account if no future reservations exist for any
     * of their accommodations. When a host deletes their account, all their
     * accommodations are also soft-deleted.
     *
     * @param userId the ID of the user to delete
     * @throws AccountDeletionException if the account cannot be deleted
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist"));

        log.info("Attempting to delete account for user {} with role {}", userId, user.getRole());

        if (user.getRole() == Role.GUEST) {
            DeletionCheckResult checkResult = reservationGrpcClient.checkGuestCanBeDeleted(userId);

            if (!checkResult.canBeDeleted()) {
                log.warn("Cannot delete guest account {}: {}", userId, checkResult.reason());
                throw new AccountDeletionException(
                        "Cannot delete account: you have " + checkResult.activeReservationCount() +
                                " active reservation(s). Please cancel or complete them before deleting your account.",
                        checkResult.activeReservationCount()
                );
            }
        } else if (user.getRole() == Role.HOST) {
            DeletionCheckResult checkResult = reservationGrpcClient.checkHostCanBeDeleted(userId);

            if (!checkResult.canBeDeleted()) {
                log.warn("Cannot delete host account {}: {}", userId, checkResult.reason());
                throw new AccountDeletionException(
                        "Cannot delete account: you have " + checkResult.activeReservationCount() +
                                " active reservation(s) on your accommodations. " +
                                "Please wait for them to complete before deleting your account.",
                        checkResult.activeReservationCount()
                );
            }

            // Cascade delete all accommodations for the host
            CascadeDeleteResult cascadeResult = accommodationGrpcClient.deleteAccommodationsByHost(userId);
            if (!cascadeResult.success()) {
                log.error("Failed to delete accommodations for host {}: {}", userId, cascadeResult.errorMessage());
                throw new RuntimeException("Failed to delete accommodations: " + cascadeResult.errorMessage());
            }
            log.info("Deleted {} accommodations for host {}", cascadeResult.deletedCount(), userId);
        }

        // Soft delete the user
        user.setDeleted(true);
        userRepository.save(user);

        log.info("Successfully deleted account for user {}", userId);
    }
}
