package com.fawry.routing.service.projection;

import java.math.BigDecimal;

public record DailyAggregate(
        BigDecimal totalAmount,
        BigDecimal totalCommission,
        long totalTransactions
) {
    public static DailyAggregate empty() {
        return new DailyAggregate(BigDecimal.ZERO, BigDecimal.ZERO, 0L);
    }
}
