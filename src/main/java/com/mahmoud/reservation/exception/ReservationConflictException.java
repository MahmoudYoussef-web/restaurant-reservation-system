package com.mahmoud.reservation.exception;

import org.springframework.http.HttpStatus;

public class ReservationConflictException extends ApiException {

    public ReservationConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "RESERVATION_CONFLICT");
    }
}