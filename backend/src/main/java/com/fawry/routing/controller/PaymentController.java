package com.fawry.routing.controller;

import com.fawry.routing.dto.request.RecommendRequest;
import com.fawry.routing.dto.request.SplitRequest;
import com.fawry.routing.dto.response.RecommendResponse;
import com.fawry.routing.dto.response.SplitResponse;
import com.fawry.routing.service.PaymentService;
import com.fawry.routing.service.idempotency.IdempotencyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "Smart payment routing endpoints")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/recommend")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('BILLER') and #request.billerId == authentication.principal.billerCode)")
    public RecommendResponse recommend(
            @Valid @RequestBody RecommendRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @AuthenticationPrincipal UserDetails principal) {
        return idempotencyService.executeOnce(
                idempotencyKey,
                "payments.recommend",
                principal.getUsername(),
                request,
                RecommendResponse.class,
                () -> paymentService.recommendAndRecord(request));
    }

    @PostMapping("/split")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('BILLER') and #request.billerId == authentication.principal.billerCode)")
    public SplitResponse split(
            @Valid @RequestBody SplitRequest request,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @AuthenticationPrincipal UserDetails principal) {
        return idempotencyService.executeOnce(
                idempotencyKey,
                "payments.split",
                principal.getUsername(),
                request,
                SplitResponse.class,
                () -> paymentService.splitAndRecord(request));
    }
}
