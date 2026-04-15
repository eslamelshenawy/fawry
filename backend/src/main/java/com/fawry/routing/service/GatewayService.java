package com.fawry.routing.service;

import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.dto.request.GatewayRequest;
import com.fawry.routing.dto.response.GatewayResponse;
import com.fawry.routing.exception.DuplicateResourceException;
import com.fawry.routing.exception.GatewayNotFoundException;
import com.fawry.routing.exception.InvalidGatewayUpdateException;
import com.fawry.routing.mapper.GatewayMapper;
import com.fawry.routing.repository.BillerQuotaUsageRepository;
import com.fawry.routing.repository.GatewayRepository;
import com.fawry.routing.service.audit.AuditAction;
import com.fawry.routing.service.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GatewayService {

    private static final String ENTITY_TYPE = "Gateway";

    private final GatewayRepository gatewayRepository;
    private final BillerQuotaUsageRepository quotaUsageRepository;
    private final GatewayMapper mapper;
    private final AuditLogger auditLogger;
    private final GatewayCatalog gatewayCatalog;
    private final Clock clock;

    public List<GatewayResponse> listAll() {
        return gatewayRepository.findAll().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public GatewayResponse get(Long id) {
        return mapper.toResponse(requireGateway(id));
    }

    @Transactional
    public GatewayResponse create(GatewayRequest request) {
        if (gatewayRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Gateway code already exists: " + request.code());
        }
        Gateway saved = gatewayRepository.save(mapper.toEntity(request));
        auditLogger.record(ENTITY_TYPE, saved.getCode(), AuditAction.CREATE,
                "Created gateway " + saved.getCode());
        gatewayCatalog.invalidate();
        return mapper.toResponse(saved);
    }

    @Transactional
    public GatewayResponse update(Long id, GatewayRequest request) {
        Gateway existing = requireGateway(id);
        if (!existing.getCode().equals(request.code())) {
            throw new InvalidGatewayUpdateException("Gateway code is immutable");
        }
        guardAgainstUnsafeLimitChange(existing, request);
        mapper.applyUpdate(existing, request);
        auditLogger.record(ENTITY_TYPE, existing.getCode(), AuditAction.UPDATE,
                "Updated gateway " + existing.getCode());
        gatewayCatalog.invalidate();
        return mapper.toResponse(existing);
    }

    @Transactional
    public void delete(Long id) {
        Gateway existing = requireGateway(id);
        existing.setActive(false);
        auditLogger.record(ENTITY_TYPE, existing.getCode(), AuditAction.DELETE,
                "Deactivated gateway " + existing.getCode());
        gatewayCatalog.invalidate();
    }

    private void guardAgainstUnsafeLimitChange(Gateway existing, GatewayRequest request) {
        LocalDate today = LocalDate.now(clock);
        BigDecimal peakUsage = quotaUsageRepository.peakUsageForGateway(existing.getId(), today);
        if (request.dailyLimit().compareTo(peakUsage) < 0) {
            throw new InvalidGatewayUpdateException(
                    "dailyLimit (" + request.dailyLimit() + ") is below today's peak usage ("
                            + peakUsage + ") — lower the limit only after midnight reset");
        }
    }

    private Gateway requireGateway(Long id) {
        return gatewayRepository.findById(id)
                .orElseThrow(() -> new GatewayNotFoundException(id));
    }
}
