package com.devoops.user.grpc;

/**
 * Result of checking whether a user can be deleted.
 *
 * @param canBeDeleted true if the user can be deleted
 * @param reason the reason if the user cannot be deleted, empty otherwise
 * @param activeReservationCount the number of active reservations blocking deletion
 */
public record DeletionCheckResult(
        boolean canBeDeleted,
        String reason,
        int activeReservationCount
) {
}
