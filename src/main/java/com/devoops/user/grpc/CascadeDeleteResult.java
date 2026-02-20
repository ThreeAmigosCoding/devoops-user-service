package com.devoops.user.grpc;

/**
 * Result of cascade deleting accommodations for a host.
 *
 * @param success true if deletion was successful
 * @param deletedCount the number of accommodations deleted
 * @param errorMessage error message if deletion failed, empty otherwise
 */
public record CascadeDeleteResult(
        boolean success,
        int deletedCount,
        String errorMessage
) {
}
