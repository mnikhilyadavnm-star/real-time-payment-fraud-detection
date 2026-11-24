package com.nikhil.payment.dto;

import com.nikhil.payment.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record TransactionResponse(
        Long id,
        String externalReference,
        String accountId,
        BigDecimal amount,
        String currency,
        String merchantId,
        String status,
        List<String> reasons,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        List<String> reasons = tx.getDecisionReasons() == null
                || tx.getDecisionReasons().isBlank()
                ? List.of()
                : Arrays.stream(tx.getDecisionReasons().split("; "))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
        return new TransactionResponse(
                tx.getId(),
                tx.getExternalReference(),
                tx.getAccountId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getMerchantId(),
                tx.getStatus() == null ? null : tx.getStatus().name(),
                reasons,
                tx.getCreatedAt());
    }
}
