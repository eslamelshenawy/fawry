package com.fawry.routing.service;

import com.fawry.routing.domain.entity.Biller;
import com.fawry.routing.domain.entity.BillerQuotaUsage;
import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.exception.QuotaExceededException;
import com.fawry.routing.repository.BillerQuotaUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 15);

    @Mock
    private BillerQuotaUsageRepository repository;

    @InjectMocks
    private QuotaService quotaService;

    private Biller biller;
    private Gateway gateway;

    @BeforeEach
    void setUp() {
        biller = Biller.builder().id(1L).code("BILL_1").name("Biller").active(true).build();
        gateway = Gateway.builder()
                .id(10L).code("GW1").name("Gateway 1")
                .dailyLimit(new BigDecimal("50000.00"))
                .minTransaction(new BigDecimal("10.00"))
                .maxTransaction(new BigDecimal("5000.00"))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("remainingQuota returns full dailyLimit when no usage row exists")
    void remainingWhenNoRow() {
        when(repository.find(biller.getId(), gateway.getId(), TODAY)).thenReturn(Optional.empty());

        BigDecimal remaining = quotaService.remainingQuota(biller, gateway, TODAY);

        assertThat(remaining).isEqualByComparingTo("50000.00");
    }

    @Test
    @DisplayName("remainingQuota subtracts amountUsed from dailyLimit")
    void remainingWhenRowExists() {
        BillerQuotaUsage usage = BillerQuotaUsage.builder()
                .biller(biller).gateway(gateway).usageDate(TODAY)
                .amountUsed(new BigDecimal("12000.00"))
                .build();
        when(repository.find(biller.getId(), gateway.getId(), TODAY)).thenReturn(Optional.of(usage));

        BigDecimal remaining = quotaService.remainingQuota(biller, gateway, TODAY);

        assertThat(remaining).isEqualByComparingTo("38000.00");
    }

    @Test
    @DisplayName("reserve creates new usage row when none exists and increments amountUsed")
    void reserveCreatesRow() {
        when(repository.findForUpdate(biller.getId(), gateway.getId(), TODAY)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(BillerQuotaUsage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BillerQuotaUsage result = quotaService.reserve(biller, gateway, TODAY, new BigDecimal("1000"));

        assertThat(result.getAmountUsed()).isEqualByComparingTo("1000");
        verify(repository).saveAndFlush(any(BillerQuotaUsage.class));
    }

    @Test
    @DisplayName("reserve increments existing usage when row already present")
    void reserveAccumulates() {
        BillerQuotaUsage existing = BillerQuotaUsage.builder()
                .biller(biller).gateway(gateway).usageDate(TODAY)
                .amountUsed(new BigDecimal("2000"))
                .build();
        when(repository.findForUpdate(biller.getId(), gateway.getId(), TODAY)).thenReturn(Optional.of(existing));

        BillerQuotaUsage result = quotaService.reserve(biller, gateway, TODAY, new BigDecimal("3000"));

        assertThat(result.getAmountUsed()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("reserve throws QuotaExceededException when amount exceeds remaining")
    void reserveExceedsQuota() {
        BillerQuotaUsage existing = BillerQuotaUsage.builder()
                .biller(biller).gateway(gateway).usageDate(TODAY)
                .amountUsed(new BigDecimal("49000"))
                .build();
        when(repository.findForUpdate(biller.getId(), gateway.getId(), TODAY)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> quotaService.reserve(biller, gateway, TODAY, new BigDecimal("2000")))
                .isInstanceOf(QuotaExceededException.class);
        assertThat(existing.getAmountUsed()).isEqualByComparingTo("49000");
    }
}
