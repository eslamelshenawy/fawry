package com.fawry.routing.dto.request;

import com.fawry.routing.domain.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotNull Role role,
        @Size(max = 64) String billerCode
) {
}
