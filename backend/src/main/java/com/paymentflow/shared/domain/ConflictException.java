package com.paymentflow.shared.domain;

/** Maps to HTTP 409 - a write conflict the client may retry. */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
