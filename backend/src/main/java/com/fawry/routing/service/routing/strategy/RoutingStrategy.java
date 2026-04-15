package com.fawry.routing.service.routing.strategy;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.enums.Urgency;
import com.fawry.routing.service.routing.RoutingContext;

import java.util.Comparator;
import java.util.List;

public interface RoutingStrategy {

    Urgency supports();

    List<Gateway> rank(List<Gateway> candidates, RoutingContext context);

    default Comparator<Gateway> thenByCode() {
        return Comparator.comparing(Gateway::getCode);
    }
}
