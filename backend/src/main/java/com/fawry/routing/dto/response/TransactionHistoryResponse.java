package com.fawry.routing.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record TransactionHistoryResponse(
        String billerCode,
        LocalDate date,
        BigDecimal totalAmount,
        BigDecimal totalCommission,
        long totalTransactions,
        List<GatewayBreakdown> breakdown,
        List<TransactionView> transactions,
        PageMeta page
) {

    @Builder
    public record GatewayBreakdown(
            String gatewayCode,
            String gatewayName,
            long transactionCount,
            BigDecimal totalAmount,
            BigDecimal totalCommission,
            BigDecimal dailyLimit,
            BigDecimal quotaUsed,
            BigDecimal quotaRemaining
    ) {}
}
