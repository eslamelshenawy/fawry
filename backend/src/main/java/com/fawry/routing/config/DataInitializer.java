package com.fawry.routing.config;

import com.fawry.routing.domain.entity.AppUser;
import com.fawry.routing.domain.enums.Role;
import com.fawry.routing.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private static final String DEFAULT_PASSWORD = "Password123!";

    @Bean
    public ApplicationRunner seedDefaultUsers(AppUserRepository userRepository,
                                              PasswordEncoder passwordEncoder) {
        return args -> {
            createIfMissing(userRepository, passwordEncoder, "admin", Role.ADMIN, null);
            createIfMissing(userRepository, passwordEncoder, "biller1", Role.BILLER, "BILL_12345");
        };
    }

    private void createIfMissing(AppUserRepository repository,
                                 PasswordEncoder encoder,
                                 String username,
                                 Role role,
                                 String billerCode) {
        if (repository.existsByUsername(username)) {
            return;
        }
        AppUser user = AppUser.builder()
                .username(username)
                .passwordHash(encoder.encode(DEFAULT_PASSWORD))
                .role(role)
                .billerCode(billerCode)
                .enabled(true)
                .build();
        repository.save(user);
        log.info("Seeded default user '{}' with role {}", username, role);
    }
}
