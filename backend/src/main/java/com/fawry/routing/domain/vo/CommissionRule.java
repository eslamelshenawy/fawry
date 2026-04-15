package com.fawry.routing.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommissionRule {

    private static final int SCALE = 2;
    private static final MathContext PRECISION = MathContext.DECIMAL128;

    @Column(name = "fixed_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal fixedFee;

    @Column(name = "percentage_fee", nullable = false, precision = 7, scale = 4)
    private BigDecimal percentageFee;

    public BigDecimal apply(BigDecimal amount) {
        BigDecimal variable = amount.multiply(percentageFee, PRECISION);
        return fixedFee.add(variable, PRECISION).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
