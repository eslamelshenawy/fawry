package com.fawry.routing.exception;

public class InvalidGatewayUpdateException extends DomainException {

    public InvalidGatewayUpdateException(String message) {
        super(ApiErrorCode.INVALID_REQUEST, message);
    }
}
