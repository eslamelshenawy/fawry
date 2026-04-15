package com.fawry.routing.exception;

public class SplitNotPossibleException extends DomainException {
    public SplitNotPossibleException(String reason) {
        super(ApiErrorCode.SPLIT_NOT_POSSIBLE, "Cannot split payment: " + reason);
    }
}
