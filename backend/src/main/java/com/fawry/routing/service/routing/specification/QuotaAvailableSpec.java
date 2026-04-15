package com.fawry.routing.service.routing.specification;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.service.QuotaService;
import com.fawry.routing.service.routing.RoutingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Order(30)
@Component
@RequiredArgsConstructor
public class QuotaAvailableSpec implements GatewaySpecification {

    private final QuotaService quotaService;

    @Override
    public boolean isSatisfiedBy(Gateway gateway, RoutingContext context) {
        BigDecimal remaining = quotaService.remainingQuota(
                context.biller(), gateway, context.requestedAt().toLocalDate());
        return remaining.compareTo(context.amount()) >= 0;
    }
}
