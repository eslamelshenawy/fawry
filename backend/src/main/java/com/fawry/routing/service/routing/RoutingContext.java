package com.fawry.routing.service.routing;

import com.fawry.routing.domain.entity.Biller;
import com.fawry.routing.domain.enums.Urgency;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Builder
public record RoutingContext(
        Biller biller,
        BigDecimal amount,
        Urgency urgency,
        ZonedDateTime requestedAt,
        boolean allowSplitting
) {
}
