package com.fawry.routing.service.projection;

import java.math.BigDecimal;

public record GatewayAggregate(
        String gatewayCode,
        String gatewayName,
        BigDecimal dailyLimit,
        long transactionCount,
        BigDecimal totalAmount,
        BigDecimal totalCommission
) {
}
