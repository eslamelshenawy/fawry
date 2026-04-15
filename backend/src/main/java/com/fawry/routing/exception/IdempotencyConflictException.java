package com.fawry.routing.exception;

public class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException(String message) {
        super(ApiErrorCode.DUPLICATE_RESOURCE, message);
    }
}
