package com.fawry.routing.domain.enums;

public enum Role {
    ADMIN,
    BILLER;

    public String authority() {
        return "ROLE_" + name();
    }
}
