package com.fawry.routing.dto.request;

import com.fawry.routing.domain.enums.Urgency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecommendRequest(
        @NotBlank @Size(max = 64) String billerId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull Urgency urgency
) {
}
