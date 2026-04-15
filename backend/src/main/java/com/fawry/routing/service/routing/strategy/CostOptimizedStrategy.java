package com.fawry.routing.service.routing.strategy;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.enums.Urgency;
import com.fawry.routing.service.routing.RoutingContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class CostOptimizedStrategy implements RoutingStrategy {

    @Override
    public Urgency supports() {
        return Urgency.CAN_WAIT;
    }

    @Override
    public List<Gateway> rank(List<Gateway> candidates, RoutingContext context) {
        BigDecimal amount = context.amount();
        Comparator<Gateway> byCommissionThenSpeed = Comparator
                .comparing((Gateway g) -> g.commissionFor(amount))
                .thenComparingInt(Gateway::getProcessingTimeMinutes)
                .thenComparing(thenByCode());
        return candidates.stream().sorted(byCommissionThenSpeed).toList();
    }
}
