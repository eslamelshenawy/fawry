package com.fawry.routing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GatewayResponse(
        Long id,
        String code,
        String name,
        BigDecimal fixedFee,
        BigDecimal percentageFee,
        BigDecimal dailyLimit,
        BigDecimal minTransaction,
        BigDecimal maxTransaction,
        int processingTimeMinutes,
        boolean available24x7,
        String availableDays,
        Integer availableFromHour,
        Integer availableToHour,
        boolean active
) {
}
