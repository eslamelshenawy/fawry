package com.fawry.routing.service.routing.strategy;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.enums.Urgency;
import com.fawry.routing.service.routing.RoutingContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class SpeedFirstStrategy implements RoutingStrategy {

    @Override
    public Urgency supports() {
        return Urgency.INSTANT;
    }

    @Override
    public List<Gateway> rank(List<Gateway> candidates, RoutingContext context) {
        BigDecimal amount = context.amount();
        Comparator<Gateway> bySpeedThenCost = Comparator
                .comparingInt(Gateway::getProcessingTimeMinutes)
                .thenComparing(g -> g.commissionFor(amount))
                .thenComparing(thenByCode());
        return candidates.stream().sorted(bySpeedThenCost).toList();
    }
}
