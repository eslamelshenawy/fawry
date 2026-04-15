package com.fawry.routing.repository;

import com.fawry.routing.domain.entity.BillerQuotaUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface BillerQuotaUsageRepository extends JpaRepository<BillerQuotaUsage, Long> {

    @Query("""
            SELECT q FROM BillerQuotaUsage q
             WHERE q.biller.id  = :billerId
               AND q.gateway.id = :gatewayId
               AND q.usageDate  = :usageDate
            """)
    Optional<BillerQuotaUsage> find(@Param("billerId") Long billerId,
                                    @Param("gatewayId") Long gatewayId,
                                    @Param("usageDate") LocalDate usageDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT q FROM BillerQuotaUsage q
             WHERE q.biller.id  = :billerId
               AND q.gateway.id = :gatewayId
               AND q.usageDate  = :usageDate
            """)
    Optional<BillerQuotaUsage> findForUpdate(@Param("billerId") Long billerId,
                                             @Param("gatewayId") Long gatewayId,
                                             @Param("usageDate") LocalDate usageDate);

    @Query("""
            SELECT COALESCE(MAX(q.amountUsed), 0)
              FROM BillerQuotaUsage q
             WHERE q.gateway.id = :gatewayId
               AND q.usageDate  = :usageDate
            """)
    BigDecimal peakUsageForGateway(@Param("gatewayId") Long gatewayId,
                                   @Param("usageDate") LocalDate usageDate);
}
