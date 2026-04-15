package com.fawry.routing.repository;

import com.fawry.routing.domain.entity.Biller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillerRepository extends JpaRepository<Biller, Long> {

    Optional<Biller> findByCode(String code);
}
