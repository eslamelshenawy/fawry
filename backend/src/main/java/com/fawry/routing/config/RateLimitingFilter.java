package com.fawry.routing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fawry.routing.exception.ApiError;
import com.fawry.routing.exception.ApiErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final List<Rule> rules = List.of(
            new Rule("POST", "/api/auth/login",    Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build()),
            new Rule("POST", "/api/payments/**",   Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build())
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Rule rule = matchingRule(request);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(bucketKey(request, rule),
                key -> Bucket.builder().addLimit(rule.bandwidth()).build());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        writeTooManyRequests(request, response);
    }

    private Rule matchingRule(HttpServletRequest request) {
        for (Rule rule : rules) {
            if (rule.method().equalsIgnoreCase(request.getMethod())
                    && pathMatcher.match(rule.pathPattern(), request.getRequestURI())) {
                return rule;
            }
        }
        return null;
    }

    private String bucketKey(HttpServletRequest request, Rule rule) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName()
                : clientIp(request);
        return rule.pathPattern() + "|" + principal;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApiError body = ApiError.builder()
                .timestamp(Instant.now())
                .status(429)
                .code(ApiErrorCode.INVALID_REQUEST.name())
                .message("Rate limit exceeded, please retry later")
                .path(request.getRequestURI())
                .build();
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private record Rule(String method, String pathPattern, Bandwidth bandwidth) {}
}
