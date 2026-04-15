package com.fawry.routing.service.routing.splitter;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.exception.SplitNotPossibleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class PaymentSplitter {

    private static final int SCALE = 2;

    public List<BigDecimal> split(BigDecimal totalAmount, Gateway gateway) {
        BigDecimal max = normalize(gateway.getMaxTransaction());
        BigDecimal min = normalize(gateway.getMinTransaction());
        BigDecimal amount = normalize(totalAmount);

        if (min == null || min.signum() <= 0) {
            throw new SplitNotPossibleException("gateway " + gateway.getCode()
                    + " has an invalid minTransaction");
        }
        if (max != null && max.compareTo(min) < 0) {
            throw new SplitNotPossibleException("gateway " + gateway.getCode()
                    + " has maxTransaction below minTransaction");
        }

        if (max == null || amount.compareTo(max) <= 0) {
            if (amount.compareTo(min) < 0) {
                throw new SplitNotPossibleException("amount is below gateway minimum");
            }
            return List.of(amount);
        }

        List<BigDecimal> chunks = new ArrayList<>();
        BigDecimal remaining = amount;

        while (remaining.compareTo(max) > 0) {
            chunks.add(max);
            remaining = remaining.subtract(max);
        }

        if (remaining.signum() > 0) {
            if (remaining.compareTo(min) < 0) {
                chunks = rebalanceTrailingRemainder(chunks, remaining, min, max);
            } else {
                chunks.add(remaining);
            }
        }
        return List.copyOf(chunks);
    }

    private List<BigDecimal> rebalanceTrailingRemainder(List<BigDecimal> chunks,
                                                        BigDecimal remainder,
                                                        BigDecimal min,
                                                        BigDecimal max) {
        if (chunks.isEmpty()) {
            throw new SplitNotPossibleException("single chunk below minimum transaction");
        }
        BigDecimal delta = min.subtract(remainder);
        BigDecimal lastFull = chunks.get(chunks.size() - 1);
        BigDecimal adjustedFull = lastFull.subtract(delta);

        if (adjustedFull.compareTo(min) < 0 || adjustedFull.compareTo(max) > 0) {
            throw new SplitNotPossibleException("cannot redistribute remainder to satisfy min/max bounds");
        }
        List<BigDecimal> rebalanced = new ArrayList<>(chunks);
        rebalanced.set(rebalanced.size() - 1, adjustedFull);
        rebalanced.add(min);
        return rebalanced;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
