package com.paymentflow.shared.domain;

/** Maps to HTTP 422 - a documented, tested business error (e.g. insufficient funds). */
public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
