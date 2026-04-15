package com.fawry.routing.service;

import com.fawry.routing.domain.entity.Biller;
import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.enums.Urgency;
import com.fawry.routing.domain.vo.CommissionRule;
import com.fawry.routing.dto.request.RecommendRequest;
import com.fawry.routing.dto.request.SplitRequest;
import com.fawry.routing.dto.response.RecommendResponse;
import com.fawry.routing.dto.response.SplitResponse;
import com.fawry.routing.service.routing.RoutingContext;
import com.fawry.routing.service.routing.RoutingDecision;
import com.fawry.routing.service.routing.RoutingService;
import com.fawry.routing.service.routing.splitter.PaymentSplitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final ZoneId CAIRO = ZoneId.of("Africa/Cairo");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-15T10:00:00Z"), CAIRO);

    @Mock private RoutingService routingService;
    @Mock private QuotaService quotaService;
    @Mock private TransactionService transactionService;

    private PaymentSplitter paymentSplitter;

    private PaymentService paymentService;

    private Biller biller;
    private Gateway gateway;

    @BeforeEach
    void setUp() {
        paymentSplitter = new PaymentSplitter();
        paymentService = new PaymentService(
                routingService, quotaService, transactionService, paymentSplitter, CLOCK);

        biller = Biller.builder().id(1L).code("BILL_1").name("B").active(true).build();
        gateway = Gateway.builder()
                .id(10L).code("GW1").name("Gateway 1")
                .commissionRule(new CommissionRule(new BigDecimal("2.00"), new BigDecimal("0.0150")))
                .dailyLimit(new BigDecimal("50000.00"))
                .minTransaction(new BigDecimal("10.00"))
                .maxTransaction(new BigDecimal("5000.00"))
                .processingTimeMinutes(0)
                .active(true)
                .build();
    }

    private RoutingDecision decisionFor(BigDecimal amount, Urgency urgency) {
        RoutingContext ctx = RoutingContext.builder()
                .biller(biller)
                .amount(amount)
                .urgency(urgency)
                .requestedAt(ZonedDateTime.now(CLOCK))
                .build();
        return new RoutingDecision(ctx, List.of(gateway));
    }

    @Test
    @DisplayName("recommendAndRecord reserves quota once and persists exactly one transaction")
    void recommendReservesOnceAndRecords() {
        RecommendRequest request = new RecommendRequest("BILL_1", new BigDecimal("1000"), Urgency.INSTANT);
        when(routingService.decide(request)).thenReturn(decisionFor(request.amount(), request.urgency()));

        RecommendResponse response = paymentService.recommendAndRecord(request);

        assertThat(response.recommendedGateway().id()).isEqualTo("GW1");
        assertThat(response.recommendedGateway().estimatedCommission()).isEqualByComparingTo("17.00");
        verify(quotaService).reserve(eq(biller), eq(gateway), any(), eq(new BigDecimal("1000")));
        verify(transactionService, times(1)).record(
                eq(biller), eq(gateway), eq(new BigDecimal("1000")),
                any(), eq(Urgency.INSTANT), eq(null));
    }

    @Test
    @DisplayName("splitAndRecord under gateway max returns one chunk without splitGroupId")
    void splitNoChunkingNeeded() {
        SplitRequest request = new SplitRequest("BILL_1", new BigDecimal("1000"), Urgency.CAN_WAIT);
        when(routingService.decideForSplit(any())).thenReturn(decisionFor(request.amount(), request.urgency()));

        SplitResponse response = paymentService.splitAndRecord(request);

        assertThat(response.requiresSplitting()).isFalse();
        assertThat(response.splitCount()).isEqualTo(1);
        assertThat(response.splitGroupId()).isNull();
        assertThat(response.splits()).hasSize(1);
        verify(quotaService, times(1))
                .reserve(eq(biller), eq(gateway), any(), eq(new BigDecimal("1000")));
        verify(transactionService, times(1)).record(
                any(), any(), any(), any(), any(), eq(null));
    }

    @Test
    @DisplayName("splitAndRecord over gateway max splits into chunks, reserves total once, records N transactions")
    void splitAcrossChunks() {
        SplitRequest request = new SplitRequest("BILL_1", new BigDecimal("12000"), Urgency.CAN_WAIT);
        when(routingService.decideForSplit(any())).thenReturn(decisionFor(request.amount(), request.urgency()));

        SplitResponse response = paymentService.splitAndRecord(request);

        assertThat(response.requiresSplitting()).isTrue();
        assertThat(response.splitCount()).isEqualTo(3);
        assertThat(response.splits())
                .extracting(BigDecimal::intValueExact)
                .containsExactly(5000, 5000, 2000);
        assertThat(response.splitGroupId()).isNotNull();

        BigDecimal expectedCommission = new BigDecimal("2.00").add(new BigDecimal("5000").multiply(new BigDecimal("0.0150")))
                .add(new BigDecimal("2.00")).add(new BigDecimal("5000").multiply(new BigDecimal("0.0150")))
                .add(new BigDecimal("2.00")).add(new BigDecimal("2000").multiply(new BigDecimal("0.0150")));
        assertThat(response.totalCommission()).isEqualByComparingTo(expectedCommission);

        verify(quotaService, times(1))
                .reserve(eq(biller), eq(gateway), any(), eq(new BigDecimal("12000")));
        verify(transactionService, times(3))
                .record(any(), any(), any(), any(), any(), any(UUID.class));
    }
}
