package com.fawry.routing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    private static final String SYSTEM_PRINCIPAL = "system";
    private static final String ANONYMOUS_PRINCIPAL = "anonymousUser";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || ANONYMOUS_PRINCIPAL.equals(auth.getName())) {
                return Optional.of(SYSTEM_PRINCIPAL);
            }
            return Optional.of(auth.getName());
        };
    }
}
