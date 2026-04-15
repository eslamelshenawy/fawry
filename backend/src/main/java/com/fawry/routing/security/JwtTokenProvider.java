package com.fawry.routing.security;

import com.fawry.routing.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;
    private final SecretKey signingKey;
    private final Duration ttl;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(properties.expirationMinutes());
    }

    public String generate(UserDetails user) {
        Instant now = Instant.now();
        List<String> roles = user.getAuthorities().stream().map(Object::toString).toList();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claims(Map.of(CLAIM_ROLES, roles))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expirationSeconds() {
        return ttl.toSeconds();
    }
}
