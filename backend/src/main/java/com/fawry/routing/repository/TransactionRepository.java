package com.fawry.routing.repository;

import com.fawry.routing.domain.entity.Transaction;
import com.fawry.routing.service.projection.DailyAggregate;
import com.fawry.routing.service.projection.GatewayAggregate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @EntityGraph(attributePaths = {"biller", "gateway"})
    @Query(value = """
            SELECT t FROM Transaction t
             WHERE t.biller.id  = :billerId
               AND t.createdAt >= :from
               AND t.createdAt <  :to
            """,
            countQuery = """
            SELECT COUNT(t) FROM Transaction t
             WHERE t.biller.id  = :billerId
               AND t.createdAt >= :from
               AND t.createdAt <  :to
            """)
    Page<Transaction> findPageByBillerWithinRange(@Param("billerId") Long billerId,
                                                  @Param("from") Instant from,
                                                  @Param("to") Instant to,
                                                  Pageable pageable);

    @Query("""
            SELECT new com.fawry.routing.service.projection.DailyAggregate(
                COALESCE(SUM(t.amount),     0),
                COALESCE(SUM(t.commission), 0),
                COUNT(t))
              FROM Transaction t
             WHERE t.biller.id  = :billerId
               AND t.createdAt >= :from
               AND t.createdAt <  :to
            """)
    DailyAggregate aggregateDaily(@Param("billerId") Long billerId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    @Query("""
            SELECT new com.fawry.routing.service.projection.GatewayAggregate(
                t.gateway.code,
                t.gateway.name,
                t.gateway.dailyLimit,
                COUNT(t),
                COALESCE(SUM(t.amount),     0),
                COALESCE(SUM(t.commission), 0))
              FROM Transaction t
             WHERE t.biller.id  = :billerId
               AND t.createdAt >= :from
               AND t.createdAt <  :to
             GROUP BY t.gateway.code, t.gateway.name, t.gateway.dailyLimit
             ORDER BY t.gateway.code
            """)
    List<GatewayAggregate> breakdownByBiller(@Param("billerId") Long billerId,
                                             @Param("from") Instant from,
                                             @Param("to") Instant to);
}
