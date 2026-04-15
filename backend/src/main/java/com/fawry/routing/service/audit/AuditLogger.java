package com.fawry.routing.service.audit;

import com.fawry.routing.domain.entity.AuditLog;
import com.fawry.routing.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogger {

    private final AuditLogRepository auditLogRepository;
    private final AuditorAware<String> auditorAware;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String entityType, String entityId, AuditAction action, String details) {
        AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action.name())
                .actor(auditorAware.getCurrentAuditor().orElse("system"))
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }
}
