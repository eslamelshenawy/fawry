package com.fawry.routing.service;

import com.fawry.routing.config.CacheConfig;
import com.fawry.routing.domain.entity.Gateway;
import com.fawry.routing.repository.GatewayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GatewayCatalog {

    private final GatewayRepository gatewayRepository;

    @Cacheable(CacheConfig.ACTIVE_GATEWAYS)
    @Transactional(readOnly = true)
    public List<Gateway> activeGateways() {
        return gatewayRepository.findAllByActiveTrue();
    }

    @CacheEvict(value = CacheConfig.ACTIVE_GATEWAYS, allEntries = true)
    public void invalidate() {
    }
}
