package com.fawry.routing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String username,
        String role,
        String billerCode
) {
}
