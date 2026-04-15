package com.fawry.routing.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "biller_quota_usage",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quota_usage",
                columnNames = {"biller_id", "gateway_id", "usage_date"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillerQuotaUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "biller_id", nullable = false)
    private Biller biller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gateway_id", nullable = false)
    private Gateway gateway;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "amount_used", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountUsed;

    @Version
    private Long version;

    public BigDecimal remaining() {
        return gateway.getDailyLimit().subtract(amountUsed);
    }

    public boolean canConsume(BigDecimal amount) {
        return remaining().compareTo(amount) >= 0;
    }

    public void consume(BigDecimal amount) {
        this.amountUsed = this.amountUsed.add(amount);
    }
}
