package com.fawry.routing.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GatewayRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal fixedFee,
        @NotNull @DecimalMin("0.0000") @Digits(integer = 3, fraction = 4) BigDecimal percentageFee,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal dailyLimit,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal minTransaction,
        @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal maxTransaction,
        @PositiveOrZero int processingTimeMinutes,
        @NotNull Boolean available24x7,
        @Size(max = 64) String availableDays,
        @Min(0) @Max(23) Integer availableFromHour,
        @Min(0) @Max(23) Integer availableToHour,
        Boolean active
) {

    @AssertTrue(message = "availableDays, availableFromHour and availableToHour are required when available24x7 is false")
    public boolean isWindowConsistent() {
        if (Boolean.TRUE.equals(available24x7)) {
            return true;
        }
        return availableDays != null && !availableDays.isBlank()
                && availableFromHour != null && availableToHour != null
                && availableFromHour < availableToHour;
    }

    @AssertTrue(message = "maxTransaction must be greater than or equal to minTransaction")
    public boolean isTransactionRangeConsistent() {
        return maxTransaction == null || minTransaction == null
                || maxTransaction.compareTo(minTransaction) >= 0;
    }
}
