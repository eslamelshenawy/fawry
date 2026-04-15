package com.fawry.routing.exception;

import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException {

    private final ApiErrorCode code;

    protected DomainException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
