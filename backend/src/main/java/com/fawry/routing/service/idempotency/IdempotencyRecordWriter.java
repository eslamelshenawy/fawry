package com.fawry.routing.service.idempotency;

import com.fawry.routing.domain.entity.IdempotencyKey;
import com.fawry.routing.domain.entity.IdempotencyRecord;
import com.fawry.routing.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyRecordWriter {

    private static final Duration RETENTION = Duration.ofHours(24);

    private final IdempotencyRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecord> saveIfAbsent(IdempotencyKey key,
                                                    String principal,
                                                    String requestHash,
                                                    String responseJson) {
        Instant now = Instant.now();
        IdempotencyRecord record = IdempotencyRecord.builder()
                .id(key)
                .principal(principal)
                .requestHash(requestHash)
                .responseStatus(200)
                .responseJson(responseJson)
                .createdAt(now)
                .expiresAt(now.plus(RETENTION))
                .build();
        try {
            return Optional.of(repository.saveAndFlush(record));
        } catch (DataIntegrityViolationException alreadyStoredByConcurrentCaller) {
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<IdempotencyRecord> findById(IdempotencyKey key) {
        return repository.findById(key);
    }
}
