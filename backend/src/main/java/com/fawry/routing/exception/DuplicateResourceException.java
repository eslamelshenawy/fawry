package com.fawry.routing.exception;

public class DuplicateResourceException extends DomainException {
    public DuplicateResourceException(String message) {
        super(ApiErrorCode.DUPLICATE_RESOURCE, message);
    }
}
