package com.fawry.routing.service.auth;

import com.fawry.routing.domain.entity.AppUser;
import com.fawry.routing.dto.request.LoginRequest;
import com.fawry.routing.dto.request.RegisterRequest;
import com.fawry.routing.dto.response.AuthResponse;
import com.fawry.routing.exception.DuplicateResourceException;
import com.fawry.routing.exception.InvalidCredentialsException;
import com.fawry.routing.repository.AppUserRepository;
import com.fawry.routing.security.AppUserPrincipal;
import com.fawry.routing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
            return toAuthResponse(principal);
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException();
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already exists: " + request.username());
        }
        AppUser user = AppUser.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .billerCode(request.billerCode())
                .enabled(true)
                .build();
        userRepository.save(user);
        return toAuthResponse(new AppUserPrincipal(user));
    }

    private AuthResponse toAuthResponse(AppUserPrincipal principal) {
        String token = tokenProvider.generate(principal);
        String role = principal.getAuthorities().iterator().next().getAuthority();
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType(TOKEN_TYPE)
                .expiresInSeconds(tokenProvider.expirationSeconds())
                .username(principal.getUsername())
                .role(role)
                .billerCode(principal.getBillerCode())
                .build();
    }
}
