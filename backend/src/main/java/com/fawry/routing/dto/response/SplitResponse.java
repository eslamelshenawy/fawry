package com.fawry.routing.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record SplitResponse(
        String selectedGateway,
        String gatewayName,
        boolean requiresSplitting,
        List<BigDecimal> splits,
        BigDecimal totalCommission,
        boolean quotaAvailable,
        int splitCount,
        UUID splitGroupId
) {
}
