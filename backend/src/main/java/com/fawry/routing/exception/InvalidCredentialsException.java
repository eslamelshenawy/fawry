package com.fawry.routing.exception;

public class InvalidCredentialsException extends DomainException {
    public InvalidCredentialsException() {
        super(ApiErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
    }
}
