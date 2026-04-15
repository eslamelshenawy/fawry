package com.fawry.routing.service.routing.specification;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.enums.Urgency;
import com.fawry.routing.service.routing.RoutingContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(20)
@Component
public class GatewayAvailableSpec implements GatewaySpecification {

    @Override
    public boolean isSatisfiedBy(Gateway gateway, RoutingContext context) {
        if (!gateway.isActive()) {
            return false;
        }
        if (context.urgency() == Urgency.INSTANT) {
            return gateway.isAvailableAt(context.requestedAt())
                    && gateway.getProcessingTimeMinutes() == 0;
        }
        return gateway.isAvailableAt(context.requestedAt());
    }
}
