package com.fawry.routing.exception;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {

    GATEWAY_NOT_FOUND(HttpStatus.NOT_FOUND),
    BILLER_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT),
    NO_ELIGIBLE_GATEWAY(HttpStatus.UNPROCESSABLE_ENTITY),
    SPLIT_NOT_POSSIBLE(HttpStatus.UNPROCESSABLE_ENTITY),
    QUOTA_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
