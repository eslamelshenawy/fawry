package com.fawry.routing.service.routing;

import com.fawry.routing.domain.entity.Gateway;

import java.util.List;

public record RoutingDecision(RoutingContext context, List<Gateway> rankedGateways) {

    public Gateway chosen() {
        return rankedGateways.get(0);
    }
}
