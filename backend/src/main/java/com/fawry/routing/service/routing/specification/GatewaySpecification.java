package com.fawry.routing.service.routing.specification;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.service.routing.RoutingContext;

@FunctionalInterface
public interface GatewaySpecification {

    boolean isSatisfiedBy(Gateway gateway, RoutingContext context);

    default GatewaySpecification and(GatewaySpecification other) {
        return (gateway, context) -> isSatisfiedBy(gateway, context) && other.isSatisfiedBy(gateway, context);
    }
}
