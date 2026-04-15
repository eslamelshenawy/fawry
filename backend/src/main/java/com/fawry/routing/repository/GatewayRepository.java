package com.fawry.routing.repository;

import com.fawry.routing.domain.entity.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GatewayRepository extends JpaRepository<Gateway, Long> {

    Optional<Gateway> findByCode(String code);

    boolean existsByCode(String code);

    List<Gateway> findAllByActiveTrue();
}
