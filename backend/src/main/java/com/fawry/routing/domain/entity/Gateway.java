package com.fawry.routing.domain.entity;

import com.fawry.routing.domain.vo.AvailabilityWindow;
import com.fawry.routing.domain.vo.CommissionRule;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;

@Entity
@Table(name = "gateway")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Gateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Embedded
    private CommissionRule commissionRule;

    @Column(name = "daily_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "min_transaction", nullable = false, precision = 15, scale = 2)
    private BigDecimal minTransaction;

    @Column(name = "max_transaction", precision = 15, scale = 2)
    private BigDecimal maxTransaction;

    @Column(name = "processing_time_minutes", nullable = false)
    private int processingTimeMinutes;

    @Embedded
    private AvailabilityWindow availability;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 64)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    public boolean acceptsAmount(BigDecimal amount) {
        if (amount.compareTo(minTransaction) < 0) {
            return false;
        }
        return maxTransaction == null || amount.compareTo(maxTransaction) <= 0;
    }

    public boolean acceptsMinimum(BigDecimal amount) {
        return amount.compareTo(minTransaction) >= 0;
    }

    public boolean isAvailableAt(ZonedDateTime moment) {
        return active && availability.isOpenAt(moment);
    }

    public BigDecimal commissionFor(BigDecimal amount) {
        return commissionRule.apply(amount);
    }
}
