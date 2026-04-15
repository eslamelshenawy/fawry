package com.fawry.routing.exception;

public class GatewayNotFoundException extends DomainException {
    public GatewayNotFoundException(Long id) {
        super(ApiErrorCode.GATEWAY_NOT_FOUND, "Gateway not found: id=" + id);
    }

    public GatewayNotFoundException(String code) {
        super(ApiErrorCode.GATEWAY_NOT_FOUND, "Gateway not found: code=" + code);
    }
}
