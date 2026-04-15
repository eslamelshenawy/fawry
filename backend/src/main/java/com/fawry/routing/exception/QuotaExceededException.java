package com.fawry.routing.exception;

public class QuotaExceededException extends DomainException {
    public QuotaExceededException(String gatewayCode) {
        super(ApiErrorCode.QUOTA_EXCEEDED, "Daily quota exhausted on gateway: " + gatewayCode);
    }
}
