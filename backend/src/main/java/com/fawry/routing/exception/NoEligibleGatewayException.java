package com.fawry.routing.exception;

public class NoEligibleGatewayException extends DomainException {
    public NoEligibleGatewayException(String reason) {
        super(ApiErrorCode.NO_ELIGIBLE_GATEWAY, "No eligible gateway: " + reason);
    }
}
