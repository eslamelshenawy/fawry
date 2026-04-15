package com.fawry.routing.service;

import com.fawry.routing.domain.entity.Biller;
import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.domain.entity.Transaction;
import com.fawry.routing.domain.enums.TransactionStatus;
import com.fawry.routing.domain.enums.Urgency;
import com.fawry.routing.dto.response.PageMeta;
import com.fawry.routing.dto.response.TransactionHistoryResponse;
import com.fawry.routing.dto.response.TransactionHistoryResponse.GatewayBreakdown;
import com.fawry.routing.dto.response.TransactionView;
import com.fawry.routing.exception.BillerNotFoundException;
import com.fawry.routing.mapper.TransactionMapper;
import com.fawry.routing.repository.BillerRepository;
import com.fawry.routing.repository.TransactionRepository;
import com.fawry.routing.service.projection.DailyAggregate;
import com.fawry.routing.service.projection.GatewayAggregate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final TransactionRepository transactionRepository;
    private final BillerRepository billerRepository;
    private final TransactionMapper transactionMapper;
    private final Clock clock;

    @Transactional
    public Transaction record(Biller biller, Gateway gateway, BigDecimal amount,
                              BigDecimal commission, Urgency urgency, UUID splitGroupId) {
        Transaction transaction = Transaction.builder()
                .biller(biller)
                .gateway(gateway)
                .amount(amount)
                .commission(commission)
                .totalCharged(amount.add(commission))
                .status(TransactionStatus.SUCCESS)
                .urgency(urgency)
                .splitGroupId(splitGroupId)
                .build();
        return transactionRepository.save(transaction);
    }

    public TransactionHistoryResponse history(String billerCode, LocalDate date, Pageable pageable) {
        Biller biller = billerRepository.findByCode(billerCode)
                .orElseThrow(() -> new BillerNotFoundException(billerCode));
        LocalDate target = date != null ? date : LocalDate.now(clock);

        Instant from = target.atStartOfDay(clock.getZone()).toInstant();
        Instant to = target.plusDays(1).atStartOfDay(clock.getZone()).toInstant();

        Pageable effective = pageable.getSort().isSorted()
                ? pageable
                : org.springframework.data.domain.PageRequest.of(
                        pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);

        DailyAggregate totals = transactionRepository.aggregateDaily(biller.getId(), from, to);
        List<GatewayAggregate> breakdown = transactionRepository.breakdownByBiller(biller.getId(), from, to);
        Page<Transaction> page = transactionRepository.findPageByBillerWithinRange(
                biller.getId(), from, to, effective);

        List<TransactionView> views = page.getContent().stream()
                .map(transactionMapper::toView)
                .toList();

        List<GatewayBreakdown> breakdownDtos = breakdown.stream()
                .map(this::toBreakdown)
                .toList();

        return TransactionHistoryResponse.builder()
                .billerCode(biller.getCode())
                .date(target)
                .totalAmount(nullToZero(totals.totalAmount()))
                .totalCommission(nullToZero(totals.totalCommission()))
                .totalTransactions(totals.totalTransactions())
                .breakdown(breakdownDtos)
                .transactions(views)
                .page(PageMeta.from(page))
                .build();
    }

    private GatewayBreakdown toBreakdown(GatewayAggregate aggregate) {
        BigDecimal used = nullToZero(aggregate.totalAmount());
        BigDecimal remaining = aggregate.dailyLimit().subtract(used).max(BigDecimal.ZERO);
        return GatewayBreakdown.builder()
                .gatewayCode(aggregate.gatewayCode())
                .gatewayName(aggregate.gatewayName())
                .transactionCount(aggregate.transactionCount())
                .totalAmount(used)
                .totalCommission(nullToZero(aggregate.totalCommission()))
                .dailyLimit(aggregate.dailyLimit())
                .quotaUsed(used)
                .quotaRemaining(remaining)
                .build();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
