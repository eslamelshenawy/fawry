package com.fawry.routing.dto.response;

import com.fawry.routing.domain.enums.TransactionStatus;
import com.fawry.routing.domain.enums.Urgency;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionView(
        Long id,
        String billerCode,
        String gatewayCode,
        String gatewayName,
        BigDecimal amount,
        BigDecimal commission,
        BigDecimal totalCharged,
        TransactionStatus status,
        Urgency urgency,
        UUID splitGroupId,
        Instant createdAt
) {
}
