package com.paymentflow.shared.domain;

/** Maps to HTTP 503 - retry budget exhausted under contention (ADR-004). */
public class ServiceUnavailableException extends DomainException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
