package com.devoops.user.exception;

import lombok.Getter;

/**
 * Exception thrown when an account cannot be deleted due to business rules.
 * For example, when a guest has active reservations or a host has future reservations.
 */
@Getter
public class AccountDeletionException extends RuntimeException {

    private final int activeReservationCount;

    public AccountDeletionException(String message, int activeReservationCount) {
        super(message);
        this.activeReservationCount = activeReservationCount;
    }
}
