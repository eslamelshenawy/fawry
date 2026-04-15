package com.fawry.routing.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record RecommendResponse(
        RecommendedGateway recommendedGateway,
        List<AlternativeGateway> alternatives
) {

    @Builder
    public record RecommendedGateway(
            String id,
            String name,
            BigDecimal estimatedCommission,
            String processingTime
    ) {}

    @Builder
    public record AlternativeGateway(
            String id,
            String name,
            BigDecimal estimatedCommission,
            String processingTime
    ) {}
}
