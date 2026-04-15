package com.fawry.routing.service.routing.strategy;

import com.fawry.routing.domain.enums.Urgency;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RoutingStrategyFactory {

    private final Map<Urgency, RoutingStrategy> byUrgency;

    public RoutingStrategyFactory(List<RoutingStrategy> strategies) {
        EnumMap<Urgency, RoutingStrategy> registry = new EnumMap<>(Urgency.class);
        for (RoutingStrategy strategy : strategies) {
            registry.put(strategy.supports(), strategy);
        }
        if (registry.size() != Urgency.values().length) {
            throw new IllegalStateException("Missing routing strategy for one or more urgency values");
        }
        this.byUrgency = Map.copyOf(registry);
    }

    public RoutingStrategy forUrgency(Urgency urgency) {
        return byUrgency.get(urgency);
    }
}
