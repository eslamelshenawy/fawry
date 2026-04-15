package com.fawry.routing.controller;

import com.fawry.routing.dto.response.TransactionHistoryResponse;
import com.fawry.routing.service.TransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Transactions", description = "Biller transaction history")
@RestController
@RequestMapping("/api/billers")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{billerId}/transactions")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('BILLER') and #billerId == authentication.principal.billerCode)")
    public TransactionHistoryResponse history(
            @PathVariable String billerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable) {
        return transactionService.history(billerId, date, pageable);
    }
}
