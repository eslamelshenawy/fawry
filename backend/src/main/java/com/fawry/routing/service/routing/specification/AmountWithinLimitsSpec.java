package com.fawry.routing.service.routing.specification;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.service.routing.RoutingContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(10)
@Component
public class AmountWithinLimitsSpec implements GatewaySpecification {

    @Override
    public boolean isSatisfiedBy(Gateway gateway, RoutingContext context) {
        if (context.allowSplitting()) {
            return gateway.acceptsMinimum(context.amount());
        }
        return gateway.acceptsAmount(context.amount());
    }
}
