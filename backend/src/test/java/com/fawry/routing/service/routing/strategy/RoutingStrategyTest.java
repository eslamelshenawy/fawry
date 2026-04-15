package com.fawry.routing.service.routing.strategy;

import com.fawry.routing.domain.entity.Biller;
import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.enums.Urgency;
import com.fawry.routing.domain.vo.AvailabilityWindow;
import com.fawry.routing.domain.vo.CommissionRule;
import com.fawry.routing.service.routing.RoutingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingStrategyTest {

    private static final ZonedDateTime NOW = ZonedDateTime.now();

    @Test
    @DisplayName("cost-optimized ranking picks the cheapest gateway first")
    void costOptimizedPicksCheapest() {
        Gateway cheapInstant = gateway("GW_CHEAP", "2.00", "0.0150", 0);
        Gateway cheapestSlow = gateway("GW_SLOW",  "5.00", "0.0080", 1440);
        Gateway midFree      = gateway("GW_FREE",  "0.00", "0.0250", 120);

        CostOptimizedStrategy strategy = new CostOptimizedStrategy();
        RoutingContext ctx = context(new BigDecimal("1000"), Urgency.CAN_WAIT);

        List<Gateway> ranked = strategy.rank(List.of(cheapInstant, cheapestSlow, midFree), ctx);

        assertThat(ranked).extracting(Gateway::getCode)
                .containsExactly("GW_SLOW", "GW_CHEAP", "GW_FREE");
    }

    @Test
    @DisplayName("speed-first ranking picks the fastest gateway first")
    void speedFirstPicksFastest() {
        Gateway instant = gateway("GW_INSTANT", "2.00", "0.0150", 0);
        Gateway slow    = gateway("GW_SLOW",    "0.00", "0.0080", 1440);
        Gateway mid     = gateway("GW_MID",     "0.00", "0.0250", 120);

        SpeedFirstStrategy strategy = new SpeedFirstStrategy();
        RoutingContext ctx = context(new BigDecimal("1000"), Urgency.INSTANT);

        List<Gateway> ranked = strategy.rank(List.of(slow, mid, instant), ctx);

        assertThat(ranked).extracting(Gateway::getCode)
                .containsExactly("GW_INSTANT", "GW_MID", "GW_SLOW");
    }

    @Test
    @DisplayName("factory resolves a strategy for each urgency")
    void factoryResolvesStrategies() {
        RoutingStrategyFactory factory = new RoutingStrategyFactory(
                List.of(new CostOptimizedStrategy(), new SpeedFirstStrategy()));

        assertThat(factory.forUrgency(Urgency.INSTANT)).isInstanceOf(SpeedFirstStrategy.class);
        assertThat(factory.forUrgency(Urgency.CAN_WAIT)).isInstanceOf(CostOptimizedStrategy.class);
    }

    private Gateway gateway(String code, String fixed, String percentage, int processingMinutes) {
        return Gateway.builder()
                .code(code)
                .name(code)
                .commissionRule(new CommissionRule(new BigDecimal(fixed), new BigDecimal(percentage)))
                .processingTimeMinutes(processingMinutes)
                .availability(AvailabilityWindow.alwaysOn())
                .active(true)
                .build();
    }

    private RoutingContext context(BigDecimal amount, Urgency urgency) {
        return RoutingContext.builder()
                .biller(Biller.builder().code("B").name("B").active(true).build())
                .amount(amount)
                .urgency(urgency)
                .requestedAt(NOW)
                .build();
    }
}
