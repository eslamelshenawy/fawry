package com.fawry.routing.service.routing.splitter;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.exception.SplitNotPossibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSplitterTest {

    private static final Comparator<BigDecimal> NUMERIC = BigDecimal::compareTo;

    private PaymentSplitter splitter;

    @BeforeEach
    void setUp() {
        splitter = new PaymentSplitter();
    }

    @Test
    @DisplayName("returns a single chunk when amount is within limits")
    void noSplitWhenWithinLimit() {
        Gateway gateway = gatewayWith("10", "5000");
        List<BigDecimal> chunks = splitter.split(new BigDecimal("3000"), gateway);
        assertThat(chunks).hasSize(1)
                .usingElementComparator(NUMERIC)
                .containsExactly(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("splits 12000 over max 5000 into 5000 + 5000 + 2000")
    void splitsOverLimitAmount() {
        Gateway gateway = gatewayWith("10", "5000");
        List<BigDecimal> chunks = splitter.split(new BigDecimal("12000"), gateway);
        assertThat(chunks)
                .usingElementComparator(NUMERIC)
                .containsExactly(
                        new BigDecimal("5000"),
                        new BigDecimal("5000"),
                        new BigDecimal("2000"));
    }

    @Test
    @DisplayName("rebalances trailing remainder below minimum by borrowing from previous chunk")
    void rebalancesTrailingRemainder() {
        Gateway gateway = gatewayWith("100", "5000");
        List<BigDecimal> chunks = splitter.split(new BigDecimal("10050"), gateway);
        assertThat(chunks)
                .usingElementComparator(NUMERIC)
                .containsExactly(
                        new BigDecimal("5000"),
                        new BigDecimal("4950"),
                        new BigDecimal("100"));
    }

    @Test
    @DisplayName("throws when amount is below the gateway minimum")
    void throwsWhenBelowMinimum() {
        Gateway gateway = gatewayWith("100", "5000");
        assertThatThrownBy(() -> splitter.split(new BigDecimal("50"), gateway))
                .isInstanceOf(SplitNotPossibleException.class);
    }

    private Gateway gatewayWith(String min, String max) {
        return Gateway.builder()
                .minTransaction(new BigDecimal(min))
                .maxTransaction(new BigDecimal(max))
                .build();
    }
}
