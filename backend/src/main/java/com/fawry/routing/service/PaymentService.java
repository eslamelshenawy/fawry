package com.fawry.routing.service;

import com.fawry.routing.domain.entity.Biller;
import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.dto.request.RecommendRequest;
import com.fawry.routing.dto.request.SplitRequest;
import com.fawry.routing.dto.response.RecommendResponse;
import com.fawry.routing.dto.response.SplitResponse;
import com.fawry.routing.service.routing.ProcessingTimeFormatter;
import com.fawry.routing.service.routing.RoutingDecision;
import com.fawry.routing.service.routing.RoutingService;
import com.fawry.routing.service.routing.splitter.PaymentSplitter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RoutingService routingService;
    private final QuotaService quotaService;
    private final TransactionService transactionService;
    private final PaymentSplitter paymentSplitter;
    private final Clock clock;

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0, maxDelay = 500))
    public RecommendResponse recommendAndRecord(RecommendRequest request) {
        requirePositive(request.amount());
        RoutingDecision decision = routingService.decide(request);
        Gateway chosen = decision.chosen();
        Biller biller = decision.context().biller();
        LocalDate today = decision.context().requestedAt().toLocalDate();
        BigDecimal amount = request.amount();

        BigDecimal commission = chosen.commissionFor(amount);
        quotaService.reserve(biller, chosen, today, amount);
        transactionService.record(biller, chosen, amount, commission, request.urgency(), null);

        return buildResponse(decision, amount);
    }

    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2.0, maxDelay = 500))
    public SplitResponse splitAndRecord(SplitRequest request) {
        requirePositive(request.amount());
        RecommendRequest routingRequest = new RecommendRequest(
                request.billerId(), request.amount(), request.urgency());
        RoutingDecision decision = routingService.decideForSplit(routingRequest);
        Gateway chosen = decision.chosen();
        Biller biller = decision.context().biller();
        LocalDate today = decision.context().requestedAt().toLocalDate();

        List<BigDecimal> chunks = paymentSplitter.split(request.amount(), chosen);
        boolean requiresSplitting = chunks.size() > 1;

        quotaService.reserve(biller, chosen, today, request.amount());

        UUID splitGroupId = requiresSplitting ? UUID.randomUUID() : null;
        BigDecimal totalCommission = BigDecimal.ZERO;
        List<BigDecimal> persistedChunks = new ArrayList<>(chunks.size());

        for (BigDecimal chunk : chunks) {
            BigDecimal chunkCommission = chosen.commissionFor(chunk);
            transactionService.record(biller, chosen, chunk, chunkCommission,
                    request.urgency(), splitGroupId);
            totalCommission = totalCommission.add(chunkCommission);
            persistedChunks.add(chunk);
        }

        return SplitResponse.builder()
                .selectedGateway(chosen.getCode())
                .gatewayName(chosen.getName())
                .requiresSplitting(requiresSplitting)
                .splits(persistedChunks)
                .totalCommission(totalCommission)
                .quotaAvailable(true)
                .splitCount(persistedChunks.size())
                .splitGroupId(splitGroupId)
                .build();
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private RecommendResponse buildResponse(RoutingDecision decision, BigDecimal amount) {
        Gateway top = decision.chosen();
        var recommended = RecommendResponse.RecommendedGateway.builder()
                .id(top.getCode())
                .name(top.getName())
                .estimatedCommission(top.commissionFor(amount))
                .processingTime(ProcessingTimeFormatter.format(top.getProcessingTimeMinutes()))
                .build();
        List<RecommendResponse.AlternativeGateway> alternatives = decision.rankedGateways().stream()
                .skip(1)
                .map(g -> RecommendResponse.AlternativeGateway.builder()
                        .id(g.getCode())
                        .name(g.getName())
                        .estimatedCommission(g.commissionFor(amount))
                        .processingTime(ProcessingTimeFormatter.format(g.getProcessingTimeMinutes()))
                        .build())
                .toList();
        return RecommendResponse.builder()
                .recommendedGateway(recommended)
                .alternatives(alternatives)
                .build();
    }
}
