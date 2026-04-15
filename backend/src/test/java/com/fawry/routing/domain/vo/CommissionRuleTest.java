package com.fawry.routing.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CommissionRuleTest {

    @Test
    @DisplayName("applies fixed fee plus percentage to the amount")
    void appliesFixedAndPercentage() {
        CommissionRule rule = new CommissionRule(new BigDecimal("2.00"), new BigDecimal("0.0150"));
        assertThat(rule.apply(new BigDecimal("1000")))
                .isEqualByComparingTo("17.00");
    }

    @Test
    @DisplayName("rounds HALF_UP to two decimals")
    void roundsHalfUp() {
        CommissionRule rule = new CommissionRule(new BigDecimal("0.00"), new BigDecimal("0.0250"));
        assertThat(rule.apply(new BigDecimal("10.01")))
                .isEqualByComparingTo("0.25");
    }

    @Test
    @DisplayName("zero fixed and percentage yields zero commission")
    void zeroRule() {
        CommissionRule rule = new CommissionRule(BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(rule.apply(new BigDecimal("9999")))
                .isEqualByComparingTo("0.00");
    }
}
