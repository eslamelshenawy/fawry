package com.fawry.routing.service.routing;

import com.fawry.routing.domain.entity.Biller;
import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.dto.request.RecommendRequest;
import com.fawry.routing.dto.response.RecommendResponse;
import com.fawry.routing.exception.BillerNotFoundException;
import com.fawry.routing.exception.NoEligibleGatewayException;
import com.fawry.routing.repository.BillerRepository;
import com.fawry.routing.service.GatewayCatalog;
import com.fawry.routing.service.routing.specification.GatewaySpecification;
import com.fawry.routing.service.routing.strategy.RoutingStrategy;
import com.fawry.routing.service.routing.strategy.RoutingStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutingService {

    private final GatewayCatalog gatewayCatalog;
    private final BillerRepository billerRepository;
    private final List<GatewaySpecification> specifications;
    private final RoutingStrategyFactory strategyFactory;
    private final RoutingMetrics metrics;
    private final Clock clock;

    public RoutingDecision decide(RecommendRequest request) {
        return decide(request, false);
    }

    public RoutingDecision decideForSplit(RecommendRequest request) {
        return decide(request, true);
    }

    private RoutingDecision decide(RecommendRequest request, boolean allowSplitting) {
        Biller biller = billerRepository.findByCode(request.billerId())
                .orElseThrow(() -> new BillerNotFoundException(request.billerId()));

        RoutingContext context = RoutingContext.builder()
                .biller(biller)
                .amount(request.amount())
                .urgency(request.urgency())
                .requestedAt(ZonedDateTime.now(clock))
                .allowSplitting(allowSplitting)
                .build();

        List<Gateway> eligible = gatewayCatalog.activeGateways().stream()
                .filter(gateway -> matchesAllSpecs(gateway, context))
                .toList();

        if (eligible.isEmpty()) {
            metrics.recordRejection("no-eligible-gateway");
            throw new NoEligibleGatewayException(
                    "no gateway satisfies amount, availability and quota constraints");
        }

        RoutingStrategy strategy = strategyFactory.forUrgency(request.urgency());
        List<Gateway> ranked = strategy.rank(eligible, context);
        metrics.recordDecision(ranked.get(0), request.urgency());
        return new RoutingDecision(context, ranked);
    }

    public RecommendResponse recommend(RecommendRequest request) {
        RoutingDecision decision = decide(request);
        return toResponse(decision);
    }

    private boolean matchesAllSpecs(Gateway gateway, RoutingContext context) {
        for (GatewaySpecification spec : specifications) {
            if (!spec.isSatisfiedBy(gateway, context)) {
                return false;
            }
        }
        return true;
    }

    private RecommendResponse toResponse(RoutingDecision decision) {
        List<Gateway> ranked = decision.rankedGateways();
        Gateway top = ranked.get(0);

        RecommendResponse.RecommendedGateway recommended = RecommendResponse.RecommendedGateway.builder()
                .id(top.getCode())
                .name(top.getName())
                .estimatedCommission(top.commissionFor(decision.context().amount()))
                .processingTime(ProcessingTimeFormatter.format(top.getProcessingTimeMinutes()))
                .build();

        List<RecommendResponse.AlternativeGateway> alternatives = ranked.stream()
                .skip(1)
                .map(g -> RecommendResponse.AlternativeGateway.builder()
                        .id(g.getCode())
                        .name(g.getName())
                        .estimatedCommission(g.commissionFor(decision.context().amount()))
                        .processingTime(ProcessingTimeFormatter.format(g.getProcessingTimeMinutes()))
                        .build())
                .toList();

        return RecommendResponse.builder()
                .recommendedGateway(recommended)
                .alternatives(alternatives)
                .build();
    }
}
