package com.fawry.routing.service.routing;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.enums.Urgency;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RoutingMetrics {

    private static final String DECISIONS = "routing.decisions";
    private static final String REJECTIONS = "routing.rejections";

    private final MeterRegistry registry;

    public RoutingMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordDecision(Gateway chosen, Urgency urgency) {
        registry.counter(DECISIONS,
                        "gateway", chosen.getCode(),
                        "urgency", urgency.name())
                .increment();
    }

    public void recordRejection(String reason) {
        registry.counter(REJECTIONS, "reason", reason).increment();
    }
}
