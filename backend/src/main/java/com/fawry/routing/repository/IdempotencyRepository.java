package com.fawry.routing.repository;

import com.fawry.routing.domain.entity.IdempotencyKey;
import com.fawry.routing.domain.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, IdempotencyKey> {
}
