package com.fawry.routing.service;

import com.fawry.routing.domain.entity.Biller;
import com.fawry.routing.domain.entity.BillerQuotaUsage;
import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.exception.QuotaExceededException;
import com.fawry.routing.repository.BillerQuotaUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final BillerQuotaUsageRepository quotaRepository;

    @Transactional(readOnly = true)
    public BigDecimal remainingQuota(Biller biller, Gateway gateway, LocalDate date) {
        return quotaRepository.find(biller.getId(), gateway.getId(), date)
                .map(BillerQuotaUsage::remaining)
                .orElseGet(gateway::getDailyLimit);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public BillerQuotaUsage reserve(Biller biller, Gateway gateway, LocalDate date, BigDecimal amount) {
        BillerQuotaUsage usage = quotaRepository.findForUpdate(biller.getId(), gateway.getId(), date)
                .orElseGet(() -> insertOrLockExisting(biller, gateway, date));

        if (!usage.canConsume(amount)) {
            throw new QuotaExceededException(gateway.getCode());
        }
        usage.consume(amount);
        return usage;
    }

    private BillerQuotaUsage insertOrLockExisting(Biller biller, Gateway gateway, LocalDate date) {
        try {
            return quotaRepository.saveAndFlush(BillerQuotaUsage.builder()
                    .biller(biller)
                    .gateway(gateway)
                    .usageDate(date)
                    .amountUsed(BigDecimal.ZERO)
                    .build());
        } catch (DataIntegrityViolationException raceWithAnotherInsert) {
            return quotaRepository.findForUpdate(biller.getId(), gateway.getId(), date)
                    .orElseThrow(() -> raceWithAnotherInsert);
        }
    }
}
