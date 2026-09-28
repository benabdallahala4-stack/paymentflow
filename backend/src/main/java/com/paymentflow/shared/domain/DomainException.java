package com.paymentflow.shared.domain;

/** Base class for business-rule violations that should map to a 4xx HTTP response. */
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
