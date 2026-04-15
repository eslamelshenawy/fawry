package com.fawry.routing.exception;

public class BillerNotFoundException extends DomainException {
    public BillerNotFoundException(String code) {
        super(ApiErrorCode.BILLER_NOT_FOUND, "Biller not found: " + code);
    }
}
